
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

from .speech_service import SpeechService
from .protobuf import userpending_pb2, infer_pb2
from .ignite import IgniteRepository
from .exception import EmptyRegisterGroupException, InvalidAccessException, UnknownBugException
from .kafka import print_assignment, on_acked_print

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# TODO move to configMap
USER_PENDING_TOPIC = 'user-pending'
INFER_VERIFICATION_TOPIC = "infer.verification"
UPLOAD_CACHE = "uploadCache"
AUTH_CACHE = "authCache"
EMBEDDINGS_CACHE = "embeddings"

class VerifyMain:
    consumer: KafkaConsumer
    producer: KafkaProducer
    speech_service: SpeechService

    def __init__(
            self,
            consumer: KafkaConsumer,
            producer: KafkaProducer,
            ignite_repository: IgniteRepository,
            speech_service: SpeechService
    ):
        self.consumer = consumer
        self.producer = producer
        self.ignite_repository = ignite_repository
        self.speech_service = speech_service
    
    def save_embeddings(self, embedding_tuples: List[Tensor]) -> None:
        torch_blob_dict: Dict[UUID, bytes] = {}
        for key, embedding in embedding_tuples:
            bo = io.BytesIO()
            torch.save(embedding, bo)
            bo.seek(0)
            blob = bo.read()
            bo.close()
            torch_blob_dict[key] = blob
        self.ignite_repository.put_all(EMBEDDINGS_CACHE, torch_blob_dict)
        logging.debug("embedding torch.saves ok")

    def load_auth_embeddings(self) -> Optional[Dict[UUID, Tensor]]:
        auth_key_dict: Dict[Any, Any] = self.ignite_repository.scan(AUTH_CACHE, only_key=True)
        auth_keys: Dict[UUID] = auth_key_dict.keys()
        if len(auth_keys) == 0:
            return None
        
        logging.debug("scan authCache ok")

        tmp_dict: Dict[UUID, io.BytesIO] = { 
            key: io.BytesIO(raw_emb)
            for key, raw_emb 
            in self.ignite_repository.scan(EMBEDDINGS_CACHE).items() 
        }
        key_embedding_dict: Dict[UUID, Tensor] = {
            key: torch.load(bo)
            for key, bo 
            in tmp_dict.items()
        }
        for _, bo in tmp_dict.items():
            bo.close()
        
        logging.debug("scan embeddings ok")

        current_embedding_keys = key_embedding_dict.keys()
        absent_keys: set[UUID] = auth_keys - current_embedding_keys

        # get all by absent_key
        absent_key_blob_dict: Dict[UUID, bytes] = self.ignite_repository.get_all(UPLOAD_CACHE, absent_keys)
        absent_auth_waveform_tuples: List[Tuple[UUID, Tensor]] = [
            (key, SpeechService.audio_blob_to_waveform(blob))
            for key, blob
            in absent_key_blob_dict.items()
            if key is not None and blob is not None
        ]
        absent_auth_waveform_tuples = [(key, wf) for key, wf in absent_auth_waveform_tuples if wf is not None]

        # absent_auth_waveform_tuples: Iterable[Tuple[Any, Tensor]] = starmap(SpeechService.blob_to_waveform, absent_key_blob_dict.items())
        logging.debug("blobs to waveforms ok")
        
        absent_auth_embedding_tuples: List[Tuple[UUID, Tensor]] = [
            (key, self.speech_service.waveform_to_embedding(waveform))
            for key, waveform
            in absent_auth_waveform_tuples
        ]
        # absent_auth_embedding_tuples: List[Tuple[Any, Tensor]] = list(map(lambda key2waveform: self.speech_service.wf_to_embedding(key2waveform[0], key2waveform[1]), absent_auth_waveform_tuples))
        logging.debug("to auth_embeddings ok ")
        
        for key, auth_embedding in absent_auth_embedding_tuples:
            key_embedding_dict[key] = auth_embedding
        
        self.save_embeddings(absent_auth_embedding_tuples)
        
        logging.debug("save auth embeddings ok")
        
        return key_embedding_dict
    
    def on_next(self, msg: Message):
        logging.debug('on_next_user_pending: msg.value = ')
        logging.debug(msg.value())
        user_pending = userpending_pb2.UserPending.FromString(msg.value())
        cand_key: str = user_pending.reqId
        user_id: str = user_pending.userId
        
        cand_key_uuid: UUID = uuid.UUID(cand_key)

        cand_blob = self.ignite_repository.get(UPLOAD_CACHE, cand_key_uuid)
        if cand_blob is None:
            raise InvalidAccessException(reqId=cand_key, userId=user_id)

        cand_waveform = SpeechService.audio_blob_to_waveform(cand_blob)
        logging.debug("prepare cand_waveform ok")
        
        
        cand_embedding = self.speech_service.waveform_to_embedding(cand_waveform)
        logging.debug("get cand_embedding ok")
        
        if cand_embedding is None:
            raise InvalidAccessException(reqId=cand_key, userId=user_id)
        
        auth_embedding_dict: Optional[Dict[UUID, Tensor]] = self.load_auth_embeddings()
        if auth_embedding_dict is None or len(auth_embedding_dict.keys()) == 0:
            raise EmptyRegisterGroupException(reqId=cand_key, userId=user_id)
        logging.debug("load_auth_embeddings ok")
        
        infer_result_list: List[Tuple[UUID, Tensor, Tensor]] = [
            (key, *self.speech_service.verify(auth_embedding, cand_embedding))
            for key, auth_embedding
            in auth_embedding_dict.items()
        ]
        # sims = map(lambda key_value: self.speech_service.verify(key_value[0], key_value[1], cand_embedding), auth_embedding_dict.items())
        logging.debug("get infer_result_list ok")
        
        try:
            match_uuid, match_score, prediction = max(infer_result_list, key=lambda infer_result: infer_result[1]) # uuid, score, prediction
            logging.debug("verify predict ok")
        except ValueError as e:
            logging.debug("value error ERRROR")
            raise EmptyRegisterGroupException(reqId=cand_key, userId=user_id)
        
        label = str(self.ignite_repository.get('uuid2label', match_uuid) if prediction else "unknown").replace(",", " ")
        infer_result = 'OK' if prediction else 'FAIL'
        score = str(match_score.item())
        additional_msg = "voice is not detected; " if match_score == torch.tensor([-1.0]) else None
        
        # Build Output Message
        message = infer_pb2.Infer()
        message.reqId = cand_key
        message.userId = user_id
        message.inferResult = infer_result
        message.score = score
        
        if prediction:
            message.label = label
        if additional_msg:
            message.info = additional_msg

        logging.debug("message = ")
        logging.debug(message)
        
        return message
    
    def run_loop(self):
        try:
            self.consumer.subscribe([USER_PENDING_TOPIC], on_assign=print_assignment)
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
                    result = self.on_next(msg)
                    result: bytes = result.SerializeToString()
                except EmptyRegisterGroupException as e:
                    result: bytes = str(e).encode('utf-8')
                except InvalidAccessException as e:
                    result: bytes = str(e).encode('utf-8')
                except UnknownBugException as e:
                    result: bytes = str(e).encode('utf-8')
                except RuntimeError as e:
                    logging.debug("ERRORRR: ")
                    logging.debug(e)
                    raise e

                if result is None:
                    logging.debug("NONEEEE: ")
                    logging.debug(("invalid payload", msg))
                    raise RuntimeError("unknown runtime error.....")

                logging.debug(result)
                self.producer.produce(topic=INFER_VERIFICATION_TOPIC, value=result, callback=on_acked_print)
                self.producer.poll(5)
                logging.debug("producer.poll ok")
                self.consumer.commit(asynchronous=True)
                logging.debug("consumer.commit ok")

                logging.debug("SEND OK")
        finally:
            self.consumer.close()
            self.producer.flush()
