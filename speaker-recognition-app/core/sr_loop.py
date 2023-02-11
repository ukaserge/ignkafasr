
import io
import logging
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List
from uuid import UUID

import torch
#torch.set_num_threads = 2
#torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from torch import Tensor
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException

from .speaker_recognition import SrService
from .protobuf.speech_request_pb2 import SpeechRequest
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .ignite_util import IgniteRepository
from .kafka_util import print_assignment, on_acked_print
from .cassandra_util import CassandraRepository

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# KAFKA
SPEECH_REQUEST_TOPIC = 'speech.request'
ANALYSIS_RESULT_SID_TOPIC = "analysis.result.sid"

# IGNITE
BLOB_CACHE = 'blobs'
KEY2NAME_CACHE = 'key2name'
EMBEDDING_CACHE = 'embeddings'

# CASSANDRA
YOUTUBE_VIDEO_TABLE = 'dj.youtube_video'
YOUTUBE_VIDEO_SID_TABLE = "dj.youtube_video_sid"

# create table dj.youtube_video_sid (video_id text PRIMARY KEY, bytes_value blob);
# Speaker Recognition Main
class SrMain:
    def __init__(
            self,
            consumer: KafkaConsumer,
            producer: KafkaProducer,
            ignite_repository: IgniteRepository,
            sr_service: SrService,
            cassandra_repository: CassandraRepository
    ):
        self.consumer = consumer
        self.producer = producer
        self.ignite_repository = ignite_repository
        self.sr_service = sr_service
        self.cassandra_repository = cassandra_repository
    
    def save_embeddings(self, embedding_tuples: List[Tensor]) -> None:
        torch_blob_dict: Dict[UUID, bytes] = {}
        for key, embedding in embedding_tuples:
            bo = io.BytesIO()
            torch.save(embedding, bo)
            bo.seek(0)
            blob = bo.read()
            bo.close()
            torch_blob_dict[key] = blob
        self.ignite_repository.put_all(EMBEDDING_CACHE, torch_blob_dict)
        logging.debug("embedding torch.saves ok")

    def load_speaker_embeddings(self) -> Optional[Dict[UUID, Tensor]]:
        logging.debug("load speaker embedding start")
        speaker_key_dict: Dict[Any, Any] = self.ignite_repository.scan(KEY2NAME_CACHE, only_key=True)
        speaker_keys = speaker_key_dict.keys()

        if len(speaker_keys) == 0:
            return None
        
        logging.debug("scan key2cache ok")
        
        # Load stored embeddings
        tmp_dict: Dict[UUID, io.BytesIO] = { 
            key: io.BytesIO(raw_emb)
            for key, raw_emb 
            in self.ignite_repository.get_all(EMBEDDING_CACHE, list(speaker_keys)).items() 
        }
        stored_embedding_dict: Dict[UUID, Tensor] = {
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
        absent_speaker_blob_dict: Dict[UUID, bytes] = self.ignite_repository.get_all(BLOB_CACHE, list(absent_keys))
        absent_speaker_waveform_tuples: List[Tuple[UUID, Tensor]] = [
            (speaker_key, SrService.audio_blob_to_waveform(blob))
            for speaker_key, blob
            in absent_speaker_blob_dict.items()
            if speaker_key is not None and blob is not None
        ]
        absent_speaker_waveform_tuples = [
            (key, waveform)
            for key, waveform 
            in absent_speaker_waveform_tuples
            if waveform is not None
        ]

        logging.debug("blobs to waveforms ok")
        
        absent_speaker_embedding_tuples: List[Tuple[UUID, Tensor]] = [
            (speaker_key, self.sr_service.waveform_to_embedding(waveform))
            for speaker_key, waveform
            in absent_speaker_waveform_tuples
        ]
        
        logging.debug("to embeddings ok ")
        
        self.save_embeddings(absent_speaker_embedding_tuples)

        for speaker_key, embedding in absent_speaker_embedding_tuples:
            stored_embedding_dict[speaker_key] = embedding
        
        logging.debug("save embeddings ok")
        logging.debug("load speaker embedding ok")

        return stored_embedding_dict
    
    def search_speaker_by_embedding(self, target_embedding, speakers_embedding_dict):
        if len(speakers_embedding_dict.keys()) == 0:
            return None, None
        verify_stream = ( 
            (speaker_key, *self.sr_service.verify(speaker_embedding, target_embedding))
            for speaker_key, speaker_embedding 
            in speakers_embedding_dict.items()
        )
        # => [('speaker_key', Tensor([0.67]), Tensor([True])), (...), (....)]

        match_speaker_key, match_score, match_prediction = max(
            verify_stream,
            key = lambda res: res[1]
        )
        match_score = match_score.item()

        match_speaker_name = self.ignite_repository.get(KEY2NAME_CACHE, match_speaker_key)
        if match_speaker_name is None:
            match_speaker_name = 'unknown'

        return match_speaker_name, match_score
    
    def build_analysis_result_sid(self, req_id, user_id, video_id, chunks_sid_result):
        analysis_result_sid = AnalysisResultSid(
            reqId = req_id,
            userId = user_id,
            videoId = video_id,
            msg = "success"
        )
        for chunk_result in chunks_sid_result:
            analysis_result_sid.chunksSidResult.append(chunk_result)
        
        return analysis_result_sid
         

    def handle_empty_group(self, req_id, user_id, video_id):
        logging.debug(f"handle_empty_group req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
        analysis_result_sid = self.build_analysis_result_sid(
            req_id = req_id,
            user_id = user_id,
            video_id = video_id,
            chunks_sid_result = [ ],
            msg = "empty group"
        )
        self.producer.produce(
            topic = ANALYSIS_RESULT_SID_TOPIC, 
            value = analysis_result_sid.SerializeToString(),
            callback = on_acked_print
        )
        self.producer.poll(3)

    def handle_invalid_cache_access(self, req_id, user_id, video_id):
        logging.debug(f"handle_invalid_cache_access req_id = {req_id}, user_id = {user_id}, video_id = {video_id}")
        analysis_result_sid = self.build_analysis_result_sid(
            req_id = req_id,
            user_id = user_id,
            video_id = video_id,
            chunks_sid_result = [ ],
            msg = "invalid cache access"
        )
        self.producer.produce(
            topic = ANALYSIS_RESULT_SID_TOPIC, 
            value = analysis_result_sid.SerializeToString(),
            callback = on_acked_print
        )
        self.producer.poll(3)

    def on_next(self, msg: Message):
        logging.debug('on_next_user_pending: msg.value = ')
        logging.debug(msg.value())
        
        speech_request = SpeechRequest.FromString(msg.value())
        req_id: str = speech_request.reqId
        user_id: str = speech_request.userId
        video_id: str = speech_request.videoId
        num_of_chunk = speech_request.numOfChunk
        Chunk_Key = lambda i: f"{video_id}_{i}"
        
        result_set = self.cassandra_repository.execute(f"SELECT * FROM {YOUTUBE_VIDEO_SID_TABLE} WHERE video_id=%s", (video_id, ))
        result_row = result_set.one()
        if result_row is not None:
            bytes_value = result_row.bytes_value
            assert bytes_value is not None
            logging.debug("use stored analysis result sid")
            
            analysis_result_sid = AnalysisResultSid.FromString(bytes_value)
            analysis_result_sid.reqId = req_id
            analysis_result_sid.userId = user_id
            analysis_result_sid.videoId = video_id

            logging.info(analysis_result_sid)
            
            self.producer.produce(
                topic = ANALYSIS_RESULT_SID_TOPIC, 
                value = analysis_result_sid.SerializeToString(),
                callback = on_acked_print
            )
            self.producer.poll(3)

            logging.debug("producer poll ok")
            return
        
        speakers_embedding_dict: Optional[Dict[Any, Tensor]] = self.load_speaker_embeddings()
        if speakers_embedding_dict is None or len(speakers_embedding_dict.keys()) == 0:
            self.handle_empty_group(req_id=req_id, user_id=user_id, video_id=video_id)
            return

        logging.debug("load speaker embeddings ok")

        chunk_key_stream = [
            Chunk_Key(i)
            for i 
            in range(num_of_chunk)
        ]
        blob_stream = self.ignite_repository.get_all(BLOB_CACHE, list(chunk_key_stream)).items()
        blob_stream = [
            blob 
            for key, blob 
            in blob_stream
            if blob is not None
        ]
        wf_stream = [
            SrService.audio_blob_to_waveform(
                blob = chunk_blob
            )
            for chunk_blob 
            in blob_stream
        ]
        emb_stream = [
            self.sr_service.waveform_to_embedding(
                waveform = chunk_waveform
            )
            for chunk_waveform 
            in wf_stream
        ]
        
        infer_results: List[Tuple[Optional[str], Optional[str]]] = [
            self.search_speaker_by_embedding(
                target_embedding = chunk_embedding, 
                speakers_embedding_dict = speakers_embedding_dict
            )
            for chunk_embedding
            in emb_stream
        ]
        infer_results = [ 
            (speaker_name, score)
            for speaker_name, score
            in infer_results
            if speaker_name is not None and score is not None
        ]

        logging.debug("INFER RESULT!!")
        logging.debug(infer_results)
        if len(infer_results) != num_of_chunk:
            self.handle_invalid_cache_access(req_id = req_id, user_id = user_id, video_id = video_id)
            return
        
        logging.debug("get infer_results ok")
        
        # produce 'analysis.result.sid'
        chunks_sid_result = [
            AnalysisResultSid.ChunkSidResult(chunkId=chunk_key, speakerName=speaker_name, score=score)
            for chunk_key, (speaker_name, score)
            in list(zip(chunk_key_stream, infer_results))
        ]
        logging.debug(chunks_sid_result)
        assert len(chunks_sid_result) == num_of_chunk

        analysis_result_sid = self.build_analysis_result_sid(
            req_id = req_id,
            user_id = user_id,
            video_id = video_id,
            chunks_sid_result = chunks_sid_result
        )
        logging.info(analysis_result_sid)

        serialized_value = analysis_result_sid.SerializeToString()
        logging.info(serialized_value)
        self.producer.produce(
            topic = ANALYSIS_RESULT_SID_TOPIC, 
            value = serialized_value,
            callback = on_acked_print
        )
        self.producer.poll(3)

        logging.debug("producer poll ok")
        
        self.cassandra_repository.execute(f"INSERT INTO {YOUTUBE_VIDEO_SID_TABLE} (video_id, bytes_value) VALUES (%s, %s)", (video_id, serialized_value))
        self.cassandra_repository.execute(f"UPDATE {YOUTUBE_VIDEO_TABLE} SET is_processed = true WHERE id = %s", (video_id, ))
        
        logging.debug("analysis result store to cassandra")
    
    def run_loop(self):
        try:
            self.consumer.subscribe([SPEECH_REQUEST_TOPIC], on_assign=print_assignment)
            while True:
                msg: Optional[Message] = self.consumer.poll(timeout=1000.0)

                if msg is None:
                    logging.debug("Wait...")
                    continue
                elif msg.error():
                    logging.debug("MSG.ERROR() !!!!")
                    raise KafkaException(msg.error())
                
                sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                 (msg.topic(), msg.partition(), msg.offset(),
                                  str(msg.key())))
                logging.debug(msg.value())

                self.consumer.store_offsets(msg)

                try:
                    self.on_next(msg)
                except Exception as e:
                    logging.debug("ERRORRR: ")
                    logging.debug(e)
                    raise e

                self.consumer.commit(asynchronous=True)
                logging.debug("consumer.commit ok")
        finally:
            self.consumer.close()
            self.producer.flush()
