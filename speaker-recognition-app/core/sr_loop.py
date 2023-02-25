import numpy as np
import io
import logging
import sys
from typing import Optional, Tuple, Any, Dict, List
from uuid import UUID
import os
import torch
import threading
from torch import Tensor
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer

from .my_constants import BLOB_CACHE, EMBEDDING_CACHE, KEY2NAME_CACHE, WHISPER_LIBNAME, WHISPER_FNAME_MODEL
from .speaker_recognition import infer_speaker_embedding, compare_embedding
from .audio_processing import load_numpy_waveform, load_waveform 
from .whisper_cpp_cdll.core import run_whisper
from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid

def save_embeddings(ignite_template, embedding_tuples: List[Tensor]):
    torch_blob_dict: Dict[UUID, bytes] = {}
    for key, embedding in embedding_tuples:
        bo = io.BytesIO()
        torch.save(embedding, bo)
        bo.seek(0)
        blob = bo.read()
        bo.close()
        torch_blob_dict[key] = blob
    ignite_template.put_all(EMBEDDING_CACHE, torch_blob_dict)
    del torch_blob_dict

    logging.debug("embedding torch.saves ok")

def load_speakers_name_and_embedding(
    ignite_template, 
    sr_session, 
    sr_f
) -> Tuple[Optional[Dict[str, Tensor]], Optional[Dict[str, str]]]:
    logging.info(
        '#%sT%s - Load Speaker Embedding Start!!',
        os.getpid(), threading.get_ident()
    )
    
    speaker_key2name: Dict[str, str] = ignite_template.scan(
        KEY2NAME_CACHE, 
        only_key=False
    )
 
    speaker_keys = speaker_key2name.keys()

    if len(speaker_keys) == 0:
        return None, None
        
    logging.info(
        '#%sT%s - Scan Ignite KEY2NAME Cache OK!!',
        os.getpid(), threading.get_ident()
    )
        
    # Load stored embeddings
    tmp_dict: Dict[str, io.BytesIO] = { 
        key: io.BytesIO(raw_emb)
        for key, raw_emb 
        in ignite_template.get_all(EMBEDDING_CACHE, list(speaker_keys)).items() 
    }
    stored_embedding_dict: Dict[str, Tensor] = {
        key: torch.load(bo)
        for key, bo 
        in tmp_dict.items()
    }
    for _, bo in tmp_dict.items():
        bo.close()
    
    logging.info(
        '#%sT%s - Load Stored Embeddings OK!!',
        os.getpid(), threading.get_ident()
    )
    
    stored_embedding_keys = stored_embedding_dict.keys()
    absent_keys: set[UUID] = speaker_keys - stored_embedding_keys
       
    logging.debug(f"absent_keys = {str(absent_keys)}")
    
    # get all by absent_key
    absent_speaker_blob_dict: Dict[str, bytes] = ignite_template.get_all(BLOB_CACHE, list(absent_keys))
    absent_speaker_waveform_tuples: List[Tuple[UUID, Tensor]] = (
        (speaker_key, load_waveform(blob))
        for speaker_key, blob
        in absent_speaker_blob_dict.items()
        if speaker_key is not None and blob is not None
    )
    absent_speaker_waveform_tuples = (
        (speaker_key, waveform)
        for speaker_key, waveform 
        in absent_speaker_waveform_tuples
        if waveform is not None
    )

    absent_speaker_embedding_tuples = [
            (speaker_key, infer_speaker_embedding(waveform, sr_session, sr_f))
            for speaker_key, waveform
            in absent_speaker_waveform_tuples
    ]
    del absent_speaker_waveform_tuples

    logging.info(
        '#%sT%s - Blob to Embeddings OK!!',
        os.getpid(), threading.get_ident()
    )
    
    save_embeddings(ignite_template, absent_speaker_embedding_tuples)
    for speaker_key, embedding in absent_speaker_embedding_tuples:
        stored_embedding_dict[speaker_key] = embedding
     
    logging.info(
        '#%sT%s - Save Embeddings OK!!',
        os.getpid(), threading.get_ident()
    ) 

    return stored_embedding_dict, speaker_key2name
 
def search_speaker_by_embedding(
    target_embedding: torch.Tensor, 
    speakers_embedding_dict: Dict[str, torch.Tensor], 
    speakers_key2name: Dict[str, str]
) -> Tuple[str, float]:
    assert len(speakers_embedding_dict.keys()) != 0
    logging.info(
        '#%sT%s - Verify Stream Start!!',
        os.getpid(), threading.get_ident()
    )
    
    verify_stream = (
        (speaker_key, *compare_embedding(speaker_embedding, target_embedding))
        for speaker_key, speaker_embedding 
        in speakers_embedding_dict.items()
    )
    # => ( ('speaker_key', Tensor([0.7]), True)), (...), (....) )
    match_speaker_key, match_score, match_prediction = max(
        verify_stream,
        key = lambda res: res[1]
    )
    # => '{speaker_key}', Tensor({float value}), Tensor({boolean value})

    logging.info(
        '#%sT%s - Verify Stream OK!!',
        os.getpid(), threading.get_ident()
    )

    match_score = match_score.item()
    match_speaker_name = speakers_key2name[match_speaker_key] if match_score >= 0.7 else 'unknown'

    return match_speaker_name, match_score

def build_analysis_result_sid(req_id, user_id, video_id, chunks_sid_result, msg):
    analysis_result_sid = AnalysisResultSid(
        reqId = req_id,
        userId = user_id,
        videoId = video_id,
        msg = msg
    )
    for chunk_result in chunks_sid_result:
        analysis_result_sid.chunksSidResult.append(chunk_result)
        
    return analysis_result_sid
     
def handle_empty_group(req_id, user_id, video_id):
    logging.info(
        '#%sT%s - Handle Empty Group!!',
        os.getpid(), threading.get_ident()
    )
    logging.debug(f"req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
    
    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = [ ],
        msg = "empty group"
    )
    return analysis_result_sid.SerializeToString(), False
    
def handle_invalid_cache_access(req_id, user_id, video_id):
    logging.info(
        '#%sT%s - Handle Invalid Access!!',
        os.getpid(), threading.get_ident()
    )
    logging.debug(f"req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
    
    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = [ ],
        msg = "invalid cache access"
     )
    return analysis_result_sid.SerializeToString(), False

def handle_voice_not_detected(req_id, user_id, video_id):
    logging.info(
        '#%sT%s - Handle Voice Not Detected!!',
        os.getpid(), threading.get_ident()
    )
    logging.debug(f"req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
    
    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = [ ],
        msg = "voice not detected"
     )
    return analysis_result_sid.SerializeToString(), False

def on_next(
    ignite_template, 
    speech_request, 
    sr_session, 
    sr_f, 
    vad_session, 
    vad_f
) -> Tuple[bytes, bool]:
    req_id: str = speech_request.reqId
    user_id: str = speech_request.userId
    video_id: str = speech_request.videoId
    num_of_chunk = speech_request.numOfChunk
    
    speakers_embedding_dict, speakers_key2name = load_speakers_name_and_embedding(ignite_template, sr_session, sr_f)
    if speakers_embedding_dict is None or len(speakers_embedding_dict.keys()) == 0:
        return handle_empty_group(req_id=req_id, user_id=user_id, video_id=video_id)

    chunk_key_stream = [
            f'{video_id}_{i}'
            for i 
            in range(num_of_chunk)
        ]

    blob_stream = ignite_template.get_all(BLOB_CACHE, chunk_key_stream).items()
    blob_stream: List[bytes] = [
            blob 
            for key, blob 
            in blob_stream
            if blob is not None
        ]

    if len(blob_stream) == 0:
        return handle_invalid_cache_access(req_id=req_id, user_id=user_id, video_id=video_id)
    
    video_waveform = np.concatenate([
            load_numpy_waveform(blob) 
            for blob 
            in blob_stream
        ])

    logging.debug("chunks -> waveform OK")
    
    spokens = run_whisper(
            data = video_waveform, 
            libname = WHISPER_LIBNAME, 
            fname_model = WHISPER_FNAME_MODEL, 
            language = b'ko'
        )
    
    logging.debug("get spokens OK")

    if len(spokens) == 0:
        return handle_voice_not_detected(req_id=req_id, user_id=user_id, video_id=video_id)

    waveform_stream = (
            torch.tensor(video_waveform[segment['start']:segment['end']]) 
            for segment
            in spokens
        )
    
    embedding_stream = (
            infer_speaker_embedding(
                chunk_waveform,
                sr_session,
                sr_f
            )
            for chunk_waveform 
            in waveform_stream
        )
    
    logging.debug(
            '#%sT%s - (on_next) Waveform to Embedding Stream Start!!',
            os.getpid(), threading.get_ident()
        )

    sid_results: List[Tuple[str, float]] = [
            search_speaker_by_embedding(
                target_embedding = chunk_embedding, 
                speakers_embedding_dict = speakers_embedding_dict,
                speakers_key2name = speakers_key2name
            )
            for chunk_embedding
            in embedding_stream
        ]

    logging.debug(
            '#%sT%s - (on_next) Blob to Embedding Stream OK!!',
            os.getpid(), threading.get_ident()
        )
    
    speakers_embedding_dict.clear()
    del speakers_embedding_dict
    
    sid_results = [
            (speaker_name, score)
            for speaker_name, score
            in sid_results
            if speaker_name is not None and score is not None
        ]
    
    logging.debug(sid_results)
    
    logging.debug("get sid_results ok")
    
    timestamp_texts = [ 
            (f"{segment['start']/16000} ~ {segment['end']/16000}", segment['text'])
            for segment
            in spokens
        ]
    
    chunks_sid_result = [
            AnalysisResultSid.ChunkSidResult(
                chunkRange=timestamp_text[0], 
                speakerName=speaker_name, 
                score=score,
                text=timestamp_text[1]
            )
            for timestamp_text, (speaker_name, score)
            in list(zip(timestamp_texts, sid_results))
        ]

    sid_results.clear()
    del sid_results
    
    logging.debug(chunks_sid_result)

    analysis_result_sid = build_analysis_result_sid(
            req_id = req_id,
            user_id = user_id,
            video_id = video_id,
            chunks_sid_result = chunks_sid_result,
            msg = "success"
        )
    logging.debug(analysis_result_sid)

    serialized_value = analysis_result_sid.SerializeToString()
    logging.debug(serialized_value)
    
    chunks_sid_result.clear()
    del chunks_sid_result
    
    return serialized_value, True

