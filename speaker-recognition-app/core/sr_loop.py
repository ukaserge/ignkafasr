
import io
import logging
import sys
from typing import Optional, Tuple, Any, Dict, List
from uuid import UUID

import torch
#torch.set_num_threads = 2
#torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from torch import Tensor
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer
from .my_constants import BLOB_CACHE, EMBEDDING_CACHE, KEY2NAME_CACHE, ONNX_FILENAME
# from .audio_processing import init_featurizer
from .speaker_recognition import get_embedding_using_onnx, verify, audio_blob_to_waveform 
from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .ignite_util import IgniteRepository

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# create table dj.youtube_video_sid (video_id text PRIMARY KEY, bytes_value blob);

def save_embeddings(ignite_repository, embedding_tuples: List[Tensor]):
    torch_blob_dict: Dict[UUID, bytes] = {}
    for key, embedding in embedding_tuples:
        bo = io.BytesIO()
        torch.save(embedding, bo)
        bo.seek(0)
        blob = bo.read()
        bo.close()
        torch_blob_dict[key] = blob

    ignite_repository.put_all(EMBEDDING_CACHE, torch_blob_dict)
    del torch_blob_dict

    logging.debug("embedding torch.saves ok")

def load_speakers_name_and_embedding(ignite_repository, session, f) -> Tuple[Optional[Dict[str, Tensor]], Optional[Dict[str, str]]]:
    logging.debug("load speaker embedding start")
    speaker_key2name: Dict[str, str] = ignite_repository.scan(KEY2NAME_CACHE)
    speaker_keys = speaker_key2name.keys()

    if len(speaker_keys) == 0:
        return None, None
        
    logging.debug("scan key2cache ok")
        
    # Load stored embeddings
    tmp_dict: Dict[str, io.BytesIO] = { 
        key: io.BytesIO(raw_emb)
        for key, raw_emb 
        in ignite_repository.get_all(EMBEDDING_CACHE, list(speaker_keys)).items() 
     }
    stored_embedding_dict: Dict[str, Tensor] = {
        key: torch.load(bo)
        for key, bo 
        in tmp_dict.items()
    }
    for _, bo in tmp_dict.items():
        bo.close()
        
    logging.debug("scan embeddings ok")
        
    stored_embedding_keys = stored_embedding_dict.keys()
    absent_keys: set[UUID] = speaker_keys - stored_embedding_keys
        
    logging.debug(f"absent_keys = {str(absent_keys)}")
    
    # get all by absent_key
    absent_speaker_blob_dict: Dict[str, bytes] = ignite_repository.get_all(BLOB_CACHE, list(absent_keys))
    absent_speaker_waveform_tuples: List[Tuple[UUID, Tensor]] = [
        (speaker_key, audio_blob_to_waveform(blob))
        for speaker_key, blob
        in absent_speaker_blob_dict.items()
        if speaker_key is not None and blob is not None
    ]
    absent_speaker_waveform_tuples = [
        (speaker_key, waveform)
        for speaker_key, waveform 
        in absent_speaker_waveform_tuples
        if waveform is not None
    ]

    logging.debug("blobs to waveforms ok")

    # model = load_model()
    # session = get_session()
    # f = get_f()
    absent_speaker_embedding_tuples: List[Tuple[UUID, Tensor]] = [
            (speaker_key, get_embedding_using_onnx(waveform, session, f))
            for speaker_key, waveform
            in absent_speaker_waveform_tuples
    ]
    del absent_speaker_waveform_tuples

    logging.debug("to embeddings ok ")
        
    save_embeddings(ignite_repository, absent_speaker_embedding_tuples)
    for speaker_key, embedding in absent_speaker_embedding_tuples:
        stored_embedding_dict[speaker_key] = embedding
     
    logging.debug("save embeddings ok")
    logging.debug("load speaker embedding ok")

    return stored_embedding_dict, speaker_key2name
 
def search_speaker_by_embedding(target_embedding, speakers_embedding_dict, speakers_key2name):
    assert len(speakers_embedding_dict.keys()) != 0

    verify_stream = (
        (speaker_key, *verify(speaker_embedding, target_embedding))
        for speaker_key, speaker_embedding 
        in speakers_embedding_dict.items()
    )
    # => ( ('speaker_key', Tensor([0.67]), True)), (...), (....) )

    match_speaker_key, match_score, match_prediction = max(
        verify_stream,
        key = lambda res: res[1]
    )
    # => '{speaker_key}', Tensor({float value}), Tensor({boolean value})

    # match_speaker_name = self.ignite_repository.get(KEY2NAME_CACHE, match_speaker_key)
    match_speaker_name = speakers_key2name[match_speaker_key]

    match_score = match_score.item()

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
    logging.debug(f"handle_empty_group req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = [ ],
        msg = "empty group"
    )
    return analysis_result_sid.SerializeToString(), False
    
def handle_invalid_cache_access(req_id, user_id, video_id):
    logging.debug(f"handle_invalid_cache_access req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = [ ],
        msg = "invalid cache access"
     )
    return analysis_result_sid.SerializeToString(), False

def on_next(ignite_repository: IgniteRepository, speech_request: SpeechRequest, session, f):
    req_id: str = speech_request.reqId
    user_id: str = speech_request.userId
    video_id: str = speech_request.videoId
    num_of_chunk = speech_request.numOfChunk
    Chunk_Key = lambda i: f"{video_id}_{i}"
    
    speakers_embedding_dict, speakers_key2name = load_speakers_name_and_embedding(ignite_repository, session, f)
    if speakers_embedding_dict is None or len(speakers_embedding_dict.keys()) == 0:
        return handle_empty_group(req_id=req_id, user_id=user_id, video_id=video_id)

    logging.debug("load speaker embeddings ok")

    chunk_key_stream = [
        Chunk_Key(i)
        for i 
        in range(num_of_chunk)
    ]
    blob_stream = ignite_repository.get_all(BLOB_CACHE, list(chunk_key_stream)).items()
    blob_stream = [
        blob 
        for key, blob 
        in blob_stream
        if blob is not None
    ]
    wf_stream = [
        audio_blob_to_waveform(
            blob = chunk_blob
        )
        for chunk_blob 
        in blob_stream
    ]
    blob_stream.clear()
    del blob_stream
    
    # model = load_model()
    #session = load_onnx_session(ONNX_FILENAME)
    #f = init_featurizer()
    # session = get_session()
    # f = get_f()
    emb_stream = [
        get_embedding_using_onnx(
            chunk_waveform,
            session,
            f
        )
        for chunk_waveform 
        in wf_stream
    ]
    #del session
    #del f
    wf_stream.clear()
    del wf_stream
    
    logging.debug("search start")
    
    infer_results: List[Tuple[Optional[str], Optional[str]]] = [
        search_speaker_by_embedding(
            target_embedding = chunk_embedding, 
            speakers_embedding_dict = speakers_embedding_dict,
            speakers_key2name = speakers_key2name
        )
        for chunk_embedding
        in emb_stream
    ]
    emb_stream.clear()
    speakers_embedding_dict.clear()
    del speakers_embedding_dict
    del emb_stream
    
    logging.debug("search ok")

    infer_results = [ 
        (speaker_name, score)
        for speaker_name, score
        in infer_results
        if speaker_name is not None and score is not None
    ]
    
    logging.debug("INFER RESULT!!")
    logging.debug(infer_results)
    
    if len(infer_results) != num_of_chunk:
        infer_results.clear()
        del infer_results
        return handle_invalid_cache_access(req_id = req_id, user_id = user_id, video_id = video_id)
    
    logging.debug("get infer_results ok")
    
    # produce 'analysis.result.sid'
    chunks_sid_result = [
        AnalysisResultSid.ChunkSidResult(chunkId=chunk_key, speakerName=speaker_name, score=score)
        for chunk_key, (speaker_name, score)
        in list(zip(chunk_key_stream, infer_results))
    ]
    infer_results.clear()
    del infer_results
    
    logging.debug(chunks_sid_result)
    assert len(chunks_sid_result) == num_of_chunk

    analysis_result_sid = build_analysis_result_sid(
        req_id = req_id,
        user_id = user_id,
        video_id = video_id,
        chunks_sid_result = chunks_sid_result,
        msg = "success"
    )
    logging.debug(analysis_result_sid)

    serialized_value = analysis_result_sid.SerializeToString()
    logging.info(serialized_value)
    
    chunks_sid_result.clear()
    del chunks_sid_result
    
    return serialized_value, True
