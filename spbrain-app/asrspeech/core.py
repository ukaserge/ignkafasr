#!/usr/bin/env python3
# import asyncio
import io
import logging
import os
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List, Iterable
from uuid import UUID
import torch
torch.set_num_threads = 2
torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from torch import Tensor
import torchaudio
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, OFFSET_BEGINNING, KafkaError, KafkaException
from pyignite import Client
import whisper
from whisper import Whisper
from speechbrain.pretrained import SpeakerRecognition
from speechbrain.pretrained import EncoderClassifier
from .protobuf import userpending_pb2, infer_pb2
# from itertools import starmap
# import contextlib
# import wave
# import numpy as np
# from scipy.spatial.distance import cdist
# from pyannote.audio import Inference
# import librosa
# import soundfile as sf
# from multiprocessing import Pool
# import speech_recognition as sr
# from pyannote.audio import Audio
# from pyannote.core import Segment
# from pyannote.audio.pipelines.speaker_verification import PretrainedSpeakerEmbedding
# from tempfile import NamedTemporaryFile

def print_assignment(topic_consumer, partitions):
    print('Assignment:', partitions)

def acked(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Message produced: %s" % (str(msg)))

class InferException(Exception):
    def __init__(self, *args, **kwargs) -> None:
        assert 'reqId' in kwargs
        assert 'userId' in kwargs
        
        self.reqId = kwargs['reqId']
        self.userId = kwargs['userId']
        
        self.info = "infer exception"
        if 'info' in kwargs:
            self.info = kwargs['info']
        
        super().__init__(*args)
    def __str__(self) -> str:
        message = infer_pb2.Infer()
        message.reqId = self.reqId
        message.inferResult = "FAIL"
        message.info = self.info
        message.userId = self.userId
        return message.SerializeToString().decode('utf-8')

class EmptyRegisterGroupException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "empty register group exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()

class InvalidAccessException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "invalid access exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()

class UnknownBugException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "unknown bug exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()

class IgniteRepository:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.ignite_client = Client()

    def get(self, cache_name: str, key: Any) -> Any:
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            return cache.get(key)
    
    def put(self, cache_name: str, key: Any, value: Any) -> None:
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            cache.put(key, value)
    
    def put_all(self, cache_name: str, key_value_dict: Dict[Any, Any]) -> None:
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            cache.put_all(key_value_dict)
    
    def get_all(self, cache_name: str, keys: Iterable[Any]) -> Dict[Any, Any]:
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            result = cache.get_all(list(keys))
        return result
    
    def scan_key_values(self, auth_cache_name: str, upload_cache_name: str):
        keys = []
        values = {}

        with self.ignite_client.connect(self.host, self.port):
            auth_cache = self.ignite_client.get_or_create_cache(auth_cache_name)
            with auth_cache.scan() as cursor:
                for k, _ in cursor:
                    keys.append(k)
                    # values.append(upload_cache.get(k))
            upload_cache = self.ignite_client.get_or_create_cache(upload_cache_name)
            values = upload_cache.get_all(keys)
        return keys, values
    
    def scan(self, cache_name: str, only_key=False) -> Dict[Any, Any]:
        key_value_dict = {}
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            if only_key:
                with cache.scan() as cursor:
                    for k, _ in cursor:
                        key_value_dict[k] = 0
            else:
                with cache.scan() as cursor:
                    key_value_dict = dict(cursor)

        return key_value_dict

class SpeechService:
    def __init__(self, embedding_model: SpeakerRecognition, asr_model: Whisper, logger: logging.Logger):
        self.embedding_model = embedding_model
        self.asr_model = asr_model
        self.logger = logger
        self.similarity = torch.nn.CosineSimilarity(dim=-1, eps=1e-6).eval()
        self.similarity.zero_grad(set_to_none=True)


    def transcribe(self, waveform: Optional[Tensor] = None):
        if waveform is None:
            raise RuntimeError("transcribe_file invalid use..")

        with torch.no_grad():
            result = self.asr_model.transcribe(waveform.squeeze(), language='en', fp16=False)
        
        return result

    def log(self, msg):
        self.logger.info(msg)

    @staticmethod
    def blob_to_waveform(key: str, blob: Optional[bytes]) -> Tuple[Optional[str], Optional[Tensor]]:
        if blob is None:
            print("blob_to_waveform None")
            return None, None
        bo = io.BytesIO(blob)
        with torch.no_grad(): 
            waveform, _ = torchaudio.load(bo)
        bo.close()
        return key, waveform

    def wf_to_embedding(self, key: Optional[str], waveform: Optional[Tensor]) -> Tuple[Optional[str], Optional[Tensor]]:
        if key is None or waveform is None:
            self.log("wf_to_embedding None arguments")
            return None, None
    
        with torch.no_grad():
            embedding: Tensor = self.embedding_model.encode_batch(waveform)
            
        return key, embedding

    def verify(self, key: Optional[str], embedding1: Optional[Tensor], embedding2: Tensor) -> Tuple[str, Tensor, Tensor]:
        if key is None or embedding1 is None:
            self.log("verify None arguments")
            return key, torch.tensor([-1.0]), torch.tensor([False])
        
        with torch.no_grad():
            score_tensor = self.similarity(embedding1, embedding2)
            prediction_tensor = score_tensor >= 0.5

        return key, score_tensor, prediction_tensor

class MainService:
    consumer: KafkaConsumer
    producer: KafkaProducer
    logger: logging.Logger
    speech_service: SpeechService

    def __init__(
            self,
            consumer: KafkaConsumer,
            producer: KafkaProducer,
            ignite_repository: IgniteRepository,
            speech_service: SpeechService,
            logger: logging.Logger
    ):
        self.consumer = consumer
        self.producer = producer
        self.ignite_repository = ignite_repository
        self.speech_service = speech_service
        self.logger = logger
        self.complete_users = set()

    def log(self, msg):
        self.logger.info(msg)
    
    def save_embeddings(self, embedding_tuples: List[Tensor]) -> None:
        torch_blob_dict: Dict[Any, bytes] = {}
        for key, embedding in embedding_tuples:
            bo = io.BytesIO()
            torch.save(embedding, bo)
            bo.seek(0)
            blob = bo.read()
            bo.close()
            torch_blob_dict[key] = blob
        self.log("embedding torch.saves ok")

        self.ignite_repository.put_all("embeddings", torch_blob_dict)

    def get_auth_embeddings(self) -> Optional[Dict[Any, Tensor]]:
        auth_key_dict: Dict[Any, Any] = self.ignite_repository.scan('authCache', only_key=True)
        auth_keys = auth_key_dict.keys()
        if len(auth_keys) == 0:
            return None
        self.log("scan authCache ok")

        tmp_dict: Dict[Any, io.BytesIO] = { 
            key: io.BytesIO(raw_emb)
            for key, raw_emb 
            in self.ignite_repository.scan('embeddings').items() 
        }
        key_embedding_dict: Dict[Any, Tensor] = {
            key: torch.load(bo)
            for key, bo 
            in tmp_dict.items()
        }
        for _, bo in tmp_dict.items():
            bo.close()
        
        self.log("scan embeddings ok")

        current_embedding_keys = key_embedding_dict.keys()
        absent_keys: set = auth_keys - current_embedding_keys

        # get all by absent_key
        absent_key_blob_dict: Dict[Any, bytes] = self.ignite_repository.get_all('uploadCache', absent_keys)
        absent_auth_waveform_tuples: Dict[Tuple[Any, Tensor]] = (
            SpeechService.blob_to_waveform(key, blob)
            for key, blob
            in absent_key_blob_dict.items()
        )
        # absent_auth_waveform_tuples: Iterable[Tuple[Any, Tensor]] = starmap(SpeechService.blob_to_waveform, absent_key_blob_dict.items())
        self.log("blobs to waveforms ok")
        
        absent_auth_waveform_tuples: Iterable[Tuple[Any, Tensor]] = (
            w_tuple 
            for w_tuple
            in absent_auth_waveform_tuples 
            if w_tuple[0] is not None
        )
        # absent_auth_waveform_tuples: Iterable[Tuple[Any, Tensor]] = filter(lambda x: x[0] is not None, absent_auth_waveform_tuples)
        self.log("filter waveform ok")
        
        absent_auth_embedding_tuples: Dict[Any, Tensor] = [
            self.speech_service.wf_to_embedding(key2waveform[0], key2waveform[1])
            for key2waveform
            in absent_auth_waveform_tuples
        ]
        # absent_auth_embedding_tuples: List[Tuple[Any, Tensor]] = list(map(lambda key2waveform: self.speech_service.wf_to_embedding(key2waveform[0], key2waveform[1]), absent_auth_waveform_tuples))
        self.log("to auth_embeddings ok ")
        
        for key, auth_embedding in absent_auth_embedding_tuples:
            key_embedding_dict[key] = auth_embedding
        self.save_embeddings(absent_auth_embedding_tuples)
        self.log("save auth embeddings ok")
        
        return key_embedding_dict
        
    def on_next_user_pending(self, msg: Message):
        self.log('on_next_user_pending: msg.value = ')
        self.log(msg.value())
        user_pending = userpending_pb2.UserPending.FromString(msg.value())
        cand_key: str = user_pending.reqId
        user_id: str = user_pending.userId
        
        cand_key_uuid: UUID = uuid.UUID(cand_key)

        cand_blob = self.ignite_repository.get('uploadCache', cand_key_uuid)
        if cand_blob is None:
            raise InvalidAccessException(reqId=cand_key, userId=user_id)

        _, cand_waveform = SpeechService.blob_to_waveform(cand_key, cand_blob)
        self.log("prepare cand_waveform ok")
        
        _, cand_embedding = self.speech_service.wf_to_embedding(cand_key_uuid, cand_waveform)
        self.log("get cand_embedding ok")
        
        if cand_embedding is None:
            raise InvalidAccessException(reqId=cand_key, userId=user_id)
        
        auth_embedding_dict: Dict[Any, Tensor] = self.get_auth_embeddings()
        if auth_embedding_dict is None or len(auth_embedding_dict.keys()) == 0:
            raise EmptyRegisterGroupException(reqId=cand_key, userId=user_id)
        self.log("get_auth_embeddings ok")
        
        sims: List[Tuple[str, Tensor, Tensor]] = [
            self.speech_service.verify(key_value[0], key_value[1], cand_embedding)
            for key_value
            in auth_embedding_dict.items()
        ]
        # sims = map(lambda key_value: self.speech_service.verify(key_value[0], key_value[1], cand_embedding), auth_embedding_dict.items())
        self.log("get sims ok")
        
        try:
            match_uuid, match_score, prediction = max(sims, key=lambda res: res[1]) # uuid, score, prediction
            self.log("verify predict ok")
        except ValueError as e:
            self.log("value error ERRROR")
            raise EmptyRegisterGroupException(reqId=cand_key, userId=user_id)
        transcribed = (self.speech_service.transcribe(waveform=cand_waveform))['text'].replace(",", " ")
        
        self.log("transcribe ok")

        label = str(self.ignite_repository.get('uuid2label', match_uuid) if prediction else "unknown").replace(",", " ")
        infer_result = 'OK' if prediction else 'FAIL'
        score = str(match_score.item())
        additional_msg = "voice is not detected; " if match_score == torch.tensor([-1.0]) else None
        
        message = infer_pb2.Infer()
        message.reqId = cand_key
        message.userId = user_id
        message.inferResult = infer_result
        message.score = score
        message.transcription = transcribed

        if prediction:
            message.label = label
        if additional_msg:
            message.info = additional_msg

        self.log("message = ")
        self.log(message)
        return message

    def run_loop(self):
        try:
            self.consumer.subscribe(['user-pending'], on_assign=print_assignment)
            while True:
                msg: Optional[Message] = self.consumer.poll(timeout=1000.0)

                if msg is None:
                    self.log("Wait...")
                    continue
                elif msg.error():
                    self.log("MSG.ERROR() !!!!")
                    raise KafkaException(msg.error())
                
                sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                 (msg.topic(), msg.partition(), msg.offset(),
                                  str(msg.key())))
                print(msg.value())
                self.consumer.store_offsets(msg)

                try:
                    result = self.on_next_user_pending(msg)
                    result: bytes = result.SerializeToString()
                except EmptyRegisterGroupException as e:
                    result: bytes = str(e).encode('utf-8')
                except InvalidAccessException as e:
                    result: bytes = str(e).encode('utf-8')
                except UnknownBugException as e:
                    result: bytes = str(e).encode('utf-8')
                except RuntimeError as e:
                    self.log("ERRORRR: ")
                    self.log(e)
                    raise e

                if result is None:
                    self.log("NONEEEE: ")
                    self.log(("invalid payload", msg))
                    raise RuntimeError("unknown runtime error.....")

                self.log(result)
                self.producer.produce(topic='infer', value=result, callback=acked)
                self.producer.poll(5)
                self.log("producer.poll ok")
                self.consumer.commit(asynchronous=True)
                self.log("consumer.commit ok")

                self.log("SEND OK")
        finally:
            self.consumer.close()
            self.producer.flush()

def commit_completed(err, partitions):
    if err:
        print(str(err))
    else:
        print("Committed partition offsets: " + str(partitions))

def load_kafka_pubsub(
        bootstrap_servers: Optional[str] = None,
        kafka_user_name: Optional[str] = None,
        kafka_user_password: Optional[str] = None,
        partition_assignment_strategy: Optional[str] = None
) -> Tuple[KafkaProducer, KafkaConsumer]:
    kafka_producer_config = {
        'bootstrap.servers': bootstrap_servers,
        'queue.buffering.max.ms': 500,
        'batch.num.messages': 50,
        'acks': 0,
        'debug': 'broker,topic,msg',
        'max.poll.interval.ms': 420000,
        'queue.buffering.max.ms': 36000,
        'linger.ms': 36000
    }
    kafka_consumer_config = {
        'bootstrap.servers': bootstrap_servers,
        'max.poll.interval.ms': 420000,
        'heartbeat.interval.ms': 10000,
        'session.timeout.ms': 30000,
        'group.id': 'uu-group',
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        'enable.auto.offset.store': False,
        # "partition.assignment.strategy": partition_assignment_strategy,
        "debug": "consumer,cgrp,topic,fetch",
        "on_commit": commit_completed
    }

    if kafka_user_name is not None:
        kafka_auth_config = {
            'sasl.mechanism': 'SCRAM-SHA-512',
            'security.protocol': 'SASL_PLAINTEXT',
            'sasl.username': kafka_user_name,
            'sasl.password': kafka_user_password,
        }
        kafka_producer_config = dict(kafka_producer_config, **kafka_auth_config)
        kafka_consumer_config = dict(kafka_consumer_config, **kafka_auth_config)
   
    plogger = logging.getLogger('consumer')
    plogger.setLevel(logging.DEBUG)
    phandler = logging.StreamHandler()
    phandler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
    plogger.addHandler(phandler)
    
    producer = KafkaProducer(kafka_producer_config, logger = plogger)
    
    logger = logging.getLogger('consumer')
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
    logger.addHandler(handler)
    consumer = KafkaConsumer(kafka_consumer_config, logger = logger)

    return producer, consumer



def load_speech_models() -> dict[str, Any]:
    models: dict[str, Any] = {
        # 'sv_model': SpeakerRecognition.from_hparams(source="speechbrain/spkrec-ecapa-voxceleb"),
        'sv_model': EncoderClassifier.from_hparams(
            source="/app/asrspeech/spkrec-ecapa-voxceleb",
            savedir="saved"
         ),
        # 'vad_model': VAD.from_hparams(source="speechbrain/vad-crdnn-libriparty",
        #                               savedir="pretrained_models/vad-crdnn-libriparty"),
        'asr_model': whisper.load_model("tiny.en", device='cpu', in_memory=True)
    }
    # get_embedding = PretrainedSpeakerEmbedding("nvidia/speakerverification_en_titanet_large")
    models['sv_model'].eval()
    models['sv_model'].zero_grad(set_to_none=True)

    models['asr_model'].eval()
    models['asr_model'].zero_grad(set_to_none=True)
    return models


def main(logger):
    logger.info("main")

    # Load Environment Variables
    try:
        bootstrap_servers = os.environ['BOOTSTRAPSERVERS']
        ignite_host = os.environ['IGNITE_SERVICE_NAME']
        ignite_port = int(os.environ['IGNITE_PORT'])
    except KeyError as e:
        logger.info("Fail to load environment variables")
        logger.info(e)
        return

    try:
        kafka_user_name = os.environ['KAFKA_USER_NAME']
        kafka_user_password = os.environ['KAFKA_USER_PASSWORD']
    except KeyError as e:
        logger.info("WARN: no security mode for local environment")
        kafka_user_name = None
        kafka_user_password = None
    partition_assignment_strategy = os.environ["CONSUMER_ASSIGN_STRATEGY"]
 
    models = load_speech_models()
    speech_service = SpeechService(embedding_model=models['sv_model'],
                                   asr_model=models['asr_model'],
                                   logger=logger)
    
    producer, consumer = load_kafka_pubsub(
            bootstrap_servers=bootstrap_servers,
            kafka_user_name=kafka_user_name,
            kafka_user_password=kafka_user_password,
            partition_assignment_strategy=partition_assignment_strategy
    )

    verify_service = MainService(
        producer=producer,
        consumer=consumer,
        ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
        speech_service=speech_service,
        logger=logger
    )

    verify_service.run_loop()

if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
        level=logging.DEBUG,
        datefmt="%H:%M:%S",
        stream=sys.stdout,
    )
    
    main_logger = logging.getLogger("main")
    logging.getLogger("chardet.charsetprober").disabled = True

    main_logger.info("logger start")
    main(main_logger)
