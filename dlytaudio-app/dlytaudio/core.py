import os
import logging
from typing import Dict 
from confluent_kafka import Producer as KafkaProducer
from pytube import YouTube
import datetime

from .my_constants import ANALYSIS_RESULT_BASIC_TOPIC, SPEECH_REQUEST_TOPIC, BLOB_CACHE, NUM_CHUNK_CACHE
from .protobuf.analysis_result_basic_pb2 import AnalysisResultBasic
from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_request_pb2 import AnalysisRequest

from .kafka_util import print_assignment, on_acked_print
from .audio_util import webm_to_wav, wav_to_chunk_blob_dict
from .youtube_util import init_youtube_object, download_webm_audio, extract_video_id

def on_next(
    analysis_request: AnalysisRequest, 
    client_loader,
    producer: KafkaProducer
):
    reqId = analysis_request.reqId
    userId = analysis_request.userId
    url = analysis_request.url 
    
    video_id = extract_video_id(url)
    if video_id is None:
        return 
    assert url.find("youtube") != -1
    
    yt: YouTube = init_youtube_object(url)
    if yt is None:
        return 
    
    analysis_result_basic = AnalysisResultBasic(
        reqId = reqId,
        userId = userId,
        url = url,
        videoId = yt.video_id,
        title = yt.title,
        description = yt.description,
        channelId = yt.channel_id,
        channelUrl = yt.channel_url,
        duration = str(yt.length),
        embedUrl = yt.embed_url,
        keywords = str(yt.keywords),
        views = str(yt.views),
        thumbnailUrl = yt.thumbnail_url
    )
    
    logging.debug("publish to analysis.result.basic START")

    producer.produce(
        topic = ANALYSIS_RESULT_BASIC_TOPIC, 
        value = analysis_result_basic.SerializeToString(), 
        callback=on_acked_print
    )
    producer.poll(3)

    logging.debug("publish to analysis.result.basic END")
    
    duration_seconds = yt.length

    # num_of_chunks = ignite_template.get(NUM_CHUNK_CACHE, video_id)
    client = client_loader()
    e = client.get(client.key(BLOB_CACHE, video_id))
    num_of_chunks = e['num_of_chunks'] if e is not None else None
    client.close()

    if num_of_chunks is None:
        logging.debug("download_wav_audio START")
        
        webm_filename = download_webm_audio(yt)
        if webm_filename is None:
            return

        wav_filename = webm_to_wav(
            src_webm_filename = webm_filename, 
            dest_wav_filename = webm_filename.replace(f"{yt.video_id}.webm", f"{yt.video_id}.wav")
        )
        
        os.remove(webm_filename)
        if wav_filename is None:
            return
        logging.debug("download_wav_audio END")

        logging.debug("build chunk_dict START")
        
        chunk_blob_dict: Dict[str, bytes] = wav_to_chunk_blob_dict(
                    wav_filename = wav_filename, 
                    prefix = f"{video_id}_",
                    chunk_size = 40 # 1280 BYTE
                )
        num_of_chunks = len(chunk_blob_dict.keys())
        
        logging.debug("build chunk_dict END")

        now = datetime.datetime.now()
        
        client = client_loader()
        entity = client.entity(key = client.key(BLOB_CACHE, video_id))
        entity['num_of_chunks'] = num_of_chunks
        entity['created_at'] = now
        client.put(entity)

        chunk_blob_entities = []
        for chunk_id, chunk_blob in chunk_blob_dict.items():
            entity = client.entity(key = client.key(BLOB_CACHE, video_id, "chunks", chunk_id))
            entity['value'] = chunk_blob
            chunk_blob_entities.append(entity)
        
        window_size = 500
        length = len(chunk_blob_entities)
        for offset in range(0, length, window_size):
            client.put_multi(chunk_blob_entities[offset:offset+window_size])
        
        client.close()

        os.remove(wav_filename)
        chunk_blob_dict.clear()
        del chunk_blob_dict
        
    assert num_of_chunks is not None
    
    logging.debug("publish speech.request START")

    speech_request = SpeechRequest(
        reqId = reqId,
        userId = userId,
        videoId = video_id,
        numOfChunk = num_of_chunks
    )
    
    producer.produce(
        topic=SPEECH_REQUEST_TOPIC, 
        value=speech_request.SerializeToString(), 
        callback=on_acked_print
    )
    producer.poll(3)

    logging.debug("publish speech.request END")
