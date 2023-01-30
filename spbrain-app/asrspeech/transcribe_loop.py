
import logging
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List
from uuid import UUID

import torch
#torch.set_num_threads = 2
#torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException

from .speech_service import SpeechService
from .protobuf import userpending_pb2, infer_pb2
from .ignite import IgniteRepository
from .exception import InvalidAccessException, UnknownBugException
from .kafka import print_assignment, on_acked_print

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# TODO refactoring duplicated constants
USER_PENDING_TOPIC = 'user-pending'
INFER_TRANSCRIPTION_TOPIC = "infer.transcription"
UPLOAD_CACHE = "uploadCache"

# TODO refactoring duplicated polling loop logic.
class TranscribeMain:
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
    
    def on_next(self, msg: Message):
        logging.debug('on_next_user_pending: msg.value = ')
        logging.debug(msg.value())
        user_pending = userpending_pb2.UserPending.FromString(msg.value())
        cand_key: str = user_pending.reqId
        user_id: str = user_pending.userId
        
        cand_key_uuid: UUID = uuid.UUID(cand_key)

        cand_blob = self.ignite_repository.get(UPLOAD_CACHE, cand_key_uuid)
        if cand_blob is None:
            logging.debug("raise invalid access exception. maybe, this msg is too old..")
            raise InvalidAccessException(reqId=cand_key, userId=user_id)

        cand_waveform = SpeechService.audio_blob_to_waveform(cand_blob)
        logging.debug("prepare cand_waveform ok")
        
        transcription = self.speech_service.transcribe(cand_waveform)
        transcription_text = transcription['text']
        logging.debug("transcribe ok")
        
        message = infer_pb2.Infer()
        message.reqId = cand_key
        message.userId = user_id
        message.transcription = transcription_text
        
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
                self.producer.produce(topic=INFER_TRANSCRIPTION_TOPIC, value=result, callback=on_acked_print)
                self.producer.poll(5)
                logging.debug("producer.poll ok")
                self.consumer.commit(asynchronous=True)
                logging.debug("consumer.commit ok")
                logging.debug("SEND OK")
        finally:
            self.consumer.close()
            self.producer.flush()
