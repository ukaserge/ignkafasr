#!/usr/bin/env python3
# import asyncio
import io
import logging
import os
import sys
# import tempfile
import uuid
from tempfile import NamedTemporaryFile
from typing import Optional, Tuple, Any
from uuid import UUID

import speech_recognition as sr
import torch
import torchaudio
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, OFFSET_BEGINNING, KafkaError, KafkaException
from pyignite import Client
# from pyignite.aio_cache import AioCache
from torch import Tensor
import whisper
from whisper import Whisper
from pyannote.audio import Audio
from pyannote.core import Segment
from pyannote.audio.pipelines.speaker_verification import PretrainedSpeakerEmbedding
import contextlib
import wave
import numpy as np
from scipy.spatial.distance import cdist
from pyannote.audio import Inference
from .protobuf import userpending_pb2, infer_pb2


class EmptyRegisterGroupException(Exception):
    def __init__(self, *args, **kwargs) -> None:
        self.reqId = kwargs['reqId']
        self.userId = kwargs['userId']
        super().__init__(*args)

    def __str__(self) -> str:
        message = infer_pb2.Infer()
        message.reqId = self.reqId
        message.inferResult = "FAIL"
        message.info = "empty register group"
        message.userId = self.userId
        message_str = message.SerializeToString().decode('utf-8')
        return message_str

class CompletedUserException(Exception):
    def __init__(self, *args, **kwargs) -> None:
        self.reqId = kwargs['reqId']
        self.userId = kwargs['userId']
        super().__init__(*args)

    def __str__(self) -> str:
        message = infer_pb2.Infer()
        message.reqId = self.reqId
        message.inferResult = "FAIL"
        message.info = "completed user"
        message.userId = self.userId
        message_str = message.SerializeToString().decode('utf-8')
        return message_str



class InvalidUuidString(RuntimeError):
    def __str__(self) -> str:
        return "invalid uuid string"


class InvalidAccessCache(RuntimeError):
    def __str__(self) -> str:
        return "can't access data ....."


class IgniteRepository:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.ignite_client = Client()

    def get(self, cache_name: str, key: Any):
        with self.ignite_client.connect(self.host, self.port):
            cache = self.ignite_client.get_or_create_cache(cache_name)
            return cache.get(key)

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


class SpeechService:
    def __init__(self, embedding_model: PretrainedSpeakerEmbedding, asr_model: Whisper, logger: logging.Logger):
        self.embedding_model = embedding_model
        self.asr_model = asr_model
        self.logger = logger


    def get_embedding(self, filename):
        self.log("get embedding")
        with contextlib.closing(wave.open(filename, 'r')) as f:
            frames = f.getnframes()
            rate = f.getframerate()
            duration = frames / float(rate)

        # self.log((frames, rate, duration))
        audio = Audio(sample_rate=16000, mono=True)
        waveform, sample_rate = audio.crop(filename, Segment(0., duration))
        embedding = self.embedding_model(waveform[None])
        return embedding

    def verify(self, filename1, filename2, threshold: int = 0.5) -> Tuple[Optional[Tensor], Optional[Tensor]]:
        embedding1 = self.get_embedding(filename1)
        embedding2 = self.get_embedding(filename2)
        distance = cdist(embedding1, embedding2, metric='cosine')
        score = 1 - distance
        prediction = score >= threshold

        return torch.tensor([score]), torch.tensor([prediction])

    def transcribe(self, filename=None, waveform: Optional[Tensor] = None):
        if waveform is not None and filename is not None:
            raise RuntimeError("transcribe_file invalid usage..")

        self.log("transcribe " + filename)
        result = self.asr_model.transcribe(audio = filename, language='en', verbose=True)
        # result = "mock"
        recorder = sr.Recognizer()
        recorder.energy_threshold = 1000
        recorder.dynamic_energy_threshold = False

        source = sr.AudioFile(filename)
        with source:
            recorder.adjust_for_ambient_noise(source)
        self.log("adjust ok")

        enhanced_audio_filename = NamedTemporaryFile().name
        with source:
            self.log("record start")
            audio1 = recorder.record(source=source, duration=source.DURATION)
            self.log("record ok")
            with open(enhanced_audio_filename, "w+b") as f:
                f.write(io.BytesIO(audio1.get_wav_data()).read())
            self.log("start self.asr_model.transcribe")
            result = self.asr_model.transcribe(enhanced_audio_filename, fp16=False, verbose=True)
            self.log(result)
        return result

    @staticmethod
    def save_blob(filename, blob):
        audio_file = open(filename, mode="w+b")
        audio_file.write(blob)
        audio_file.close()
        return filename

    def log(self, msg):
        self.logger.info(msg)


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

    def prepare_cand_audio(self, cache_key: UUID):
        cand_blob = self.ignite_repository.get('uploadCache', cache_key)
        if cand_blob is None:
            raise InvalidAccessCache()
        cand_audio_filename = self.speech_service.save_blob(filename="cand.wav", blob=cand_blob)
        return cand_audio_filename

    def prepare_auth_audio(self, auth_uuid, auth_blob):
        if auth_uuid == '' or auth_uuid == b'' or auth_uuid is None:
            raise InvalidUuidString()
        # auth_blob = await self.ignite_repository.get('uploadCache', auth_uuid)
        if auth_blob is None:
            raise InvalidAccessCache()
        auth_audio_filename = self.speech_service.save_blob(filename="auth.wav", blob=auth_blob)
        return auth_audio_filename

    def on_next_user_pending(self, msg: Message):
        self.log('on_next_user_pending: msg.value = ')
        self.log(msg.value())
        user_pending = userpending_pb2.UserPending.FromString(msg.value())
        cand_key: str = user_pending.reqId
        user_id: str = user_pending.userId
        
        # if user_id in self.complete_user:
        #   raise CompletedUserException(reqId=cand_key, userId=user_id)

        cand_key_uuid: UUID = uuid.UUID(cand_key)
        
        cand_audio_filename = self.prepare_cand_audio(cand_key_uuid)
        auth_uuids, auth_blobs = self.ignite_repository.scan_key_values('authCache', 'uploadCache')

        if len(auth_uuids) == 0:
            raise EmptyRegisterGroupException(reqId=cand_key, userId=user_id)

        flag, match_uuid, match_score = False, None, torch.tensor([-1.0])
        for auth_uuid, auth_blob in auth_blobs.items():
            try:
                auth_audio_filename = self.prepare_auth_audio(auth_uuid, auth_blob)
            except (InvalidUuidString, InvalidAccessCache) as e:
                self.log((auth_uuid, e))
                continue

            try:
                score, prediction = self.speech_service.verify(cand_audio_filename, auth_audio_filename)
            except RuntimeError as e:
                self.log(("verify fail ", e))
                continue
            except wave.Error as e:
                self.log(("wave error ", e))
                continue

            self.log((score, prediction))
            match_score = max(score, match_score)

            if prediction[0]:
                self.log("GOOD")
                flag, match_uuid, match_score = True, auth_uuid, score
                break

        transcribed = (self.speech_service.transcribe(filename=cand_audio_filename))['text'].replace(",", " ")
        label = str(self.ignite_repository.get('uuid2label', match_uuid) if flag else "unknown").replace(",", " ")
        infer_result = 'OK' if flag else 'FAIL'
        score = str(match_score.item())
        additional_msg = "voice is not detected; " if match_score == torch.tensor([-1.0]) else None
        
        message = infer_pb2.Infer()
        message.reqId = cand_key
        message.userId = user_id
        message.inferResult = infer_result
        message.score = score
        message.transcription = transcribed

        if flag:
            message.label = label
            pass
        if additional_msg:
            message.info = additional_msg
        # self.complete_user.add(user_id)

        return message

    def on_next_register_pending(self, msg: Message):
        pass

    def start_async(
            self,
            sleep=0
    ):
        try:
            self.consumer.subscribe(['user-pending'])
            # , on_assign=reset_offset_beginning)
            while True:
                msg: Optional[Message] = self.consumer.poll(timeout=1.0)

                if msg is None:
                    continue
                elif msg.error() and msg.error().code() == KafkaError._PARTITION_EOF:
                    self.log((msg.topic(), msg.partition(), msg.offset()))
                    continue
                elif msg.error():
                    self.log(msg.error())
                    continue

                try:
                    result = self.on_next_user_pending(msg)
                    result: bytes = result.SerializeToString()
                except EmptyRegisterGroupException as e:
                    result: bytes = str(e).encode('utf-8')
                #except CompletedUserException as e:
                #    result: bytes = str(e).encode('utf-8')
                except RuntimeError as e:
                    self.log(e)
                    continue
                if result is None:
                    self.log(("invalid payload", msg))
                    continue
                self.log(result)
                self.producer.produce(topic='infer', value=result)
                self.producer.poll(1)
                self.consumer.commit()
                self.log("SEND OK")
        finally:
            self.consumer.close()
            self.producer.flush()

def load_kafka_pubsub(bootstrap_servers: Optional[str] = None,
                            kafka_user_name: Optional[str] = None,
                            kafka_user_password: Optional[str] = None) -> Tuple[KafkaProducer, KafkaConsumer]:
    kafka_producer_config = {
        'bootstrap.servers': bootstrap_servers,
        'queue.buffering.max.ms': 500,
        'batch.num.messages': 50,
    }
    kafka_consumer_config = {
        'bootstrap.servers': bootstrap_servers,
        # 'max.poll.interval.ms': 60000,
        'enable.auto.commit': True,
        'group.id': 'my-group',
        'auto.offset.reset': 'latest'
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

    producer = KafkaProducer(kafka_producer_config)
    consumer = KafkaConsumer(kafka_consumer_config)

    return producer, consumer


def reset_offset_beginning(topic_consumer, partitions):
    for p in partitions:
        p.offset = OFFSET_BEGINNING
    topic_consumer.assign(partitions)


def load_speech_models() -> dict[str, Any]:
    models: dict[str, Any] = {
        'sv_model': PretrainedSpeakerEmbedding("speechbrain/spkrec-ecapa-voxceleb"),
        # 'vad_model': VAD.from_hparams(source="speechbrain/vad-crdnn-libriparty",
        #                               savedir="pretrained_models/vad-crdnn-libriparty"),
        'asr_model': whisper.load_model("tiny.en", in_memory=True)
    }
    # get_embedding = PretrainedSpeakerEmbedding("nvidia/speakerverification_en_titanet_large")
    # https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb
    # models['sv_model'].eval()
    # models['vad_model'].eval()
    # models['asr_model'].eval()

    return models


def main(logger):
    logger.info("main")

    # models = await load_speech_models()
    # speech_service = SpeechService(embedding_model=models['sv_model'],
    #                                asr_model=models['asr_model'],
    #                                logger=logger)
    # ret = speech_service.verify("112.wav", "409.wav")
    # print(ret)
    # return

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

    producer, consumer = load_kafka_pubsub(bootstrap_servers=bootstrap_servers,
                                                 kafka_user_name=kafka_user_name,
                                                 kafka_user_password=kafka_user_password)
    models = load_speech_models()
    speech_service = SpeechService(embedding_model=models['sv_model'],
                                   asr_model=models['asr_model'],
                                   logger=logger)

    verify_service = MainService(
        producer=producer,
        consumer=consumer,
        ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
        speech_service=speech_service,
        logger=logger
    )

    verify_service.start_async()


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
