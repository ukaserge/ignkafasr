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

from google.protobuf.timestamp_pb2 import Timestamp

from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .my_constants import BLOB_CACHE, EMBEDDING_CACHE, KEY2NAME_CACHE, WHISPER_LIBNAME, WHISPER_FNAME_MODEL
from .speaker_recognition import infer_speaker_embedding, search_speaker_by_embedding
from .audio_processing import load_numpy_waveform, load_waveform 
from .whisper_cpp_cdll.core import run_whisper

"""HELPER FUNCTIONS"""

def torch_to_blob(t):
    bo = io.BytesIO()
    torch.save(t, bo)
    bo.seek(0)
    blob = bo.read()
    bo.close()
    return blob

def blob_to_torch(blob):
    bo = io.BytesIO(blob)
    t = torch.load(bo)
    bo.close()
    return t

def load_joined_blob(entity, parent_kind_name, client):
    logging.debug(entity)
    n = entity['num_of_chunks']
    keys = [
        client.key(parent_kind_name, entity.key.name, 'chunks', f'{entity.key.name}_{i}')
        for i in range(n)
    ]
    keys = [keys[s:s+500] for s in range(0, n, 500)]
    chunks = []
    for keyss in keys:
        chunks.extend(client.get_multi(keyss))
    sorted_chunks = sorted(chunks, key = lambda chunk: int(chunk.key.name.replace(f"{entity.key.name}_", "")))
    blob = b''.join([chunk['value'] for chunk in sorted_chunks])
    return blob

def load_joined_waveform(entity, parent_kind_name, client):
    logging.debug(entity)
    n = entity['num_of_chunks']
    keys = [
        client.key(parent_kind_name, entity.key.name, 'chunks', f'{entity.key.name}_{i}')
        for i in range(n)
    ]
    keys = [keys[s:s+500] for s in range(0, n, 500)]
    chunks = []
    for keyss in keys:
        chunks.extend(client.get_multi(keyss))
    sorted_chunks = sorted(chunks, key = lambda chunk: int(chunk.key.name.replace(f"{entity.key.name}_", "")))

    return np.concatenate([load_numpy_waveform(chunk['value']) for chunk in sorted_chunks])
    



def load_speakers_name_and_embedding(
    client,
    sr_session, 
    sr_f
) -> Tuple[Optional[Dict[str, Tensor]], Optional[Dict[str, str]]]:
    logging.info(
        '#%sT%s - Load Speaker Embedding Start!!',
        os.getpid(), threading.get_ident()
    )
    SPEAKER_BLOBS = 'speaker-blobs'
    SPEAKER_EMBEDDINGS = 'speaker-embeddings'

    q = client.query(kind=SPEAKER_BLOBS)
    es = list(q.fetch())
    speaker_key2name = { e.key.name: e['name'] for e in es }
    
    # speaker_keys = speaker_key2name.keys()
    speaker_keys = [e.key.name for e in es]
    if len(speaker_keys) == 0:
        return None, None
    
    logging.info(
        '#%sT%s - Scan Ignite KEY2NAME Cache OK!!',
        os.getpid(), threading.get_ident()
    )
    stored_embedding_dict = { 
        entity.key.name: blob_to_torch(load_joined_blob(entity, SPEAKER_EMBEDDINGS, client))
        for entity 
        in client.get_multi([
            client.key(SPEAKER_EMBEDDINGS, speaker_key) 
            for speaker_key 
            in speaker_keys
        ]) 
    }

    stored_embedding_keys = stored_embedding_dict.keys()
    # get all by absent_key
    es2 = [e for e in es if e.key.name not in stored_embedding_keys]
    # speaker_tuples = [(e.key.name, e['name'], b''.join([chunk['value'] for chunk in sorted(client.get_multi([client.key(SPEAKER_BLOBS, e.key.name, 'chunks', f'{e.key.name}_{i}') for i in range(e['num_of_chunks'])]), key = lambda c: int(c.key.name.replace(f"{e.key.name}_", "")))])) for e in es2]
    speaker_tuples = [
        (
            e.key.name, 
            e['name'], 
            infer_speaker_embedding
            (
                load_waveform(
                    load_joined_blob(
                        e, 
                        SPEAKER_BLOBS,
                        client
                    )
                ),
                sr_session,
                sr_f
            )
        ) 
        for e 
        in es2
    ]

    # speaker_tuples = [(speaker_key, name, load_waveform(blob)) for speaker_key, name, blob in speaker_tuples]
    # speaker_tuples = [(speaker_key, name, infer_speaker_embedding(waveform, sr_session, sr_f)) for speaker_key, name, waveform in speaker_tuples]
    
    for speaker_key, name, embedding in speaker_tuples:
        entity = client.entity()
        entity.key = client.key(SPEAKER_EMBEDDINGS, speaker_key)
        entity['name'] = name

        emb_blob = torch_to_blob(embedding)
        n = len(emb_blob)
        CHUNK_SIZE = 1400
        num_of_chunks = int(n / CHUNK_SIZE) + 1
        
        entity['num_of_chunks'] = num_of_chunks
        client.put(entity)

        chunk_entities = []
        chunk_blobs = [emb_blob[s:s+CHUNK_SIZE] for s in range(0, n, CHUNK_SIZE)]
        for i, chunk_blob in enumerate(chunk_blobs):
            e = client.entity()
            e.key = client.key(SPEAKER_EMBEDDINGS, speaker_key, 'chunks', f'{speaker_key}_{i}')
            e['value'] = chunk_blob
            chunk_entities.append(e)
        client.put_multi(chunk_entities)

    """
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

    save_embeddings(ignite_template, absent_speaker_embedding_tuples)
    """

    logging.info(
        '#%sT%s - Blob to Embeddings OK!!',
        os.getpid(), threading.get_ident()
    )
    
    for speaker_key, name, embedding in speaker_tuples:
        stored_embedding_dict[speaker_key] = embedding
     
    logging.info(
        '#%sT%s - Save Embeddings OK!!',
        os.getpid(), threading.get_ident()
    ) 

    return stored_embedding_dict, speaker_key2name
 
def build_analysis_result_sid(req_id, user_id, video_id, chunks_sid_result, msg):
    timestamp = Timestamp()
    timestamp.GetCurrentTime()

    analysis_result_sid = AnalysisResultSid(
        reqId = req_id,
        userId = user_id,
        videoId = video_id,
        msg = msg,
        timestamp = timestamp.ToMicroseconds()
    )
    for chunk_result in chunks_sid_result:
        analysis_result_sid.chunksSidResult.append(chunk_result)
        
    return analysis_result_sid

"""CORE FUNCTION"""
def on_next(
    speech_request, 
    client_loader,
    sr_session, 
    sr_f
) -> Tuple[bytes, bool]:
    req_id: str = speech_request.reqId
    user_id: str = speech_request.userId
    video_id: str = speech_request.videoId
    num_of_chunk = speech_request.numOfChunk
    
    client = client_loader()
    speakers_embedding_dict, speakers_key2name = load_speakers_name_and_embedding(client, sr_session, sr_f)
    if speakers_embedding_dict is None or len(speakers_embedding_dict.keys()) == 0:
        return handle_empty_group(req_id=req_id, user_id=user_id, video_id=video_id)
    
    
    video_entity = client.get(client.key('blobs', video_id))
    if video_entity is None:
        logging.error("invalid access")
        return handle_invalid_cache_access(req_id=req_id, user_id=user_id, video_id=video_id)
    video_waveform = load_joined_waveform(video_entity, 'blobs', client)
    # video_waveform = load_numpy_waveform(video_blob)

    #blob_stream = ignite_template.get_all(
    #        BLOB_CACHE, 
    #        [f"{video_id}_{i}" for i in range(num_of_chunk)]
    #    ).items()
    #blob_stream: List[bytes] = [
    #        blob 
    #        for key, blob 
    #        in blob_stream
    #        if blob is not None
    #    ]
    # if len(blob_stream) == 0:
    #     return handle_invalid_cache_access(req_id=req_id, user_id=user_id, video_id=video_id)
    
    #video_waveform = np.concatenate([
    #        load_numpy_waveform(blob) 
    #        for blob 
    #        in blob_stream
    #    ]).copy()

    logging.debug("chunks -> waveform OK")
    
    spokens = run_whisper(
            data = video_waveform, 
            libname = WHISPER_LIBNAME, 
            fname_model = WHISPER_FNAME_MODEL, 
            verbose = True,
            n_threads = 2,
            language = b'ko',
            print_realtime = False,
            print_progress = False,
            print_timestamps = False,
            max_tokens = 10,
            max_len = 5,
            beam_search_beam_size = 10,
            greedy_best_of = -1,
            temperature = 0.0,
            speed_up = False 
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

"""EXCEPTION HANDLING FUNCTIONS"""

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


