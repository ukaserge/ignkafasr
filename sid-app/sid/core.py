import numpy as np
import io
import logging
import sys
from typing import Optional, Tuple, Any, Dict, List
import os
import torch
import threading
from torch import Tensor
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer

from google.protobuf.timestamp_pb2 import Timestamp

from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .my_constants import BLOB_CACHE, EMBEDDING_CACHE, KEY2NAME_CACHE #, WHISPER_LIBNAME, WHISPER_FNAME_MODEL
from .speaker_recognition import infer_speaker_embedding, search_speaker_by_embedding
from .torch_util import torch_to_blob, blob_to_torch
from .audio_processing import load_numpy_waveform, load_waveform, join_wav_blobs
from .whisper_api_util import run_whisper_api


"""HELPER FUNCTIONS"""

def load_joined_blob_from_datastore(entity, parent_kind_name, client):
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

def load_joined_wav_blob_from_datastore(entity, parent_kind_name, client):
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
    
    joined_blob = join_wav_blobs([chunk['value'] for chunk in sorted_chunks])
    
    return joined_blob

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
        entity.key.name: blob_to_torch(load_joined_blob_from_datastore(entity, SPEAKER_EMBEDDINGS, client))
        for entity 
        in client.get_multi([
            client.key(SPEAKER_EMBEDDINGS, speaker_key) 
            for speaker_key 
            in speaker_keys
        ]) 
    }

    stored_embedding_keys = stored_embedding_dict.keys()
    es2 = [e for e in es if e.key.name not in stored_embedding_keys]
    speaker_tuples = [
        (
            e.key.name, 
            e['name'], 
            infer_speaker_embedding
            (
                load_waveform(
                    load_joined_blob_from_datastore(
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
    SAMPLE_RATE = 16000
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

    video_blob = load_joined_wav_blob_from_datastore(video_entity, 'blobs', client)
    video_waveform = load_numpy_waveform(video_blob)
    video_waveform_size = video_waveform.size

    logging.debug("chunks -> waveform OK")
    
    spokens = run_whisper_api(video_blob) 
        
    logging.debug("get spokens OK")
    
    spokens = [
        segment 
        for segment in spokens 
        if int(segment['start']*SAMPLE_RATE)+1 < min(int(segment['end']*SAMPLE_RATE), video_waveform_size)
    ]

    if len(spokens) == 0:
        return handle_voice_not_detected(req_id=req_id, user_id=user_id, video_id=video_id)

    waveform_stream = [
            torch.tensor(video_waveform[int(segment['start']*SAMPLE_RATE):int(segment['end']*SAMPLE_RATE)] ) 
            for segment
            in spokens
        ]
    
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
            (f"{int(segment['start']/60)}:{int(segment['start']%60)} ~ {int(segment['end']/60)}:{int(segment['end']%60)}", segment['text'])
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


