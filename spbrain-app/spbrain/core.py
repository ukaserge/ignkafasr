#!/usr/bin/env python3
import asyncio
import io
import logging
import os
import sys
import tempfile
import uuid
from itertools import product
from tempfile import NamedTemporaryFile
from typing import Optional, Tuple, List, Any
from uuid import UUID

import speech_recognition as sr
import torch
import torchaudio
import whisper
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, OFFSET_BEGINNING
from pyignite import AioClient
from pyignite.aio_cache import AioCache
from speechbrain.dataio.dataio import read_audio
from speechbrain.pretrained import SpeakerRecognition, VAD
from torch import Tensor
from whisper import Whisper


class IgniteRepository:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.ignite_client = AioClient()

    async def get(self, cache_name: str, key: Any):
        async with self.ignite_client.connect(self.host, self.port):
            cache: AioCache = await self.ignite_client.get_cache(cache_name)
            return await cache.get(key)

    async def scan_keys(self, cache_name: str):
        keys = []
        async with self.ignite_client.connect(self.host, self.port):
            cache: AioCache = await self.ignite_client.get_cache(cache_name)
            async with cache.scan() as cursor:
                async for k, _ in cursor:
                    keys.append(k)
        return keys


class SpeechService:
    def __init__(self, sv_model: SpeakerRecognition, vad_model: VAD, asr_model: Whisper, logger: logging.Logger):
        self.sv_model = sv_model
        self.vad_model = vad_model
        self.asr_model = asr_model
        self.logger = logger

    async def verify_file(self, filename1, filename2):
        seg1 = self.file_to_vad_segments(filename1)
        seg2 = self.file_to_vad_segments(filename2)

        if len(seg1) == 0 or len(seg2) == 0:
            self.log("voice not detected:\n{0}\n{1}".format(str(seg1), str(seg2)))
            return torch.tensor([[-1.0]]), torch.tensor([[False]])

        ret: Tuple[Tensor, Tensor] = (torch.tensor([[-1.0]]), torch.tensor([[False]]))
        for s1, s2 in product(seg1, seg2):
            score, prediction = self.sv_model.verify_batch(s1.squeeze(), s2.squeeze())
            self.log((score, prediction))
            if score > ret[0]:
                ret = (score, prediction)
        return ret


    async def transcribe(self, filename=None, waveform: Optional[Tensor] = None):
        if waveform is not None and filename is not None:
            raise RuntimeError("transcribe_file invalid usage..")
        elif waveform is not None:
            self.log(waveform.shape)
            filename = "foo.wav"
            torchaudio.save(filepath=filename, src=waveform, sample_rate=16000, encoding="PCM_S")
            self.log(waveform.shape)

        self.log("save as " + filename)
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
            result = self.asr_model.transcribe(enhanced_audio_filename, fp16=False)
            self.log(result)

        return result

    def file_to_vad_segments(self, file: str) -> List[Tensor]:
        seg = self.vad_model.get_speech_segments(file, small_chunk_size=2, large_chunk_size=60)
        self.log(seg)

        if len(seg) == 0:
            return []

        ret = []
        seg = seg.flatten()
        for s, e in seg.reshape((len(seg) // 2, 2)):
            ret.append(read_audio({
                "file": file,
                "start": int(s.item() * 16000.0),
                "stop": int(e.item() * 16000.0)
            }))
            # print_waveform(ret[-1])
        self.log(ret)
        return ret

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

    def log(self, msg):
        self.logger.info(msg)

    def produce(self, topic, msg):
        self.producer.produce(topic='infer', value=msg)
        self.producer.poll(0)
        self.consumer.commit()
        pass

    async def on_next_user_pending(self, msg: Message) -> Optional[str]:
        # noinspection PyArgumentList
        try:
            cache_key: UUID = uuid.UUID(msg.value().decode('utf-8'))
        except Exception as e:
            self.log('user-pending invalid uuid value')
            self.log(msg.value())
            self.log(e)
            return None

        self.log('consume')
        self.log(cache_key)

        cand_blob = await self.ignite_repository.get('uploadCache', cache_key)

        if cand_blob is None:
            message = str(cache_key) + ",FAIL; cand ignite uploadCache get fail....."
            return message

        # audio_filename = NamedTemporaryFile(suffix=".wav").name
        cand_audio_filename = "cand.wav"
        audio_file = open(cand_audio_filename, mode="w+b")
        audio_file.write(cand_blob)
        audio_file.close()

        self.log("transcribe start1")
        transcribed = await self.speech_service.transcribe(filename=cand_audio_filename)
        self.log(transcribed)

        keys = await self.ignite_repository.scan_keys('authCache')
        if len(keys) == 0:
            message = str(cache_key) + ",FAIL; registered user group is empty."
            return message

        flag = False
        auth_uuid = None
        match_score = torch.tensor([-1.0])
        for key in keys:
            if key == '' or key == b'' or key is None:
                continue
            auth_blob = await self.ignite_repository.get('uploadCache', key)
            if auth_blob is None:
                continue

            auth_audio_filename = "auth.wav"
            audio_file = open(auth_audio_filename, mode="w+b")
            audio_file.write(auth_blob)
            audio_file.close()

            try:
                score, prediction = await self.speech_service.verify_file(cand_audio_filename, auth_audio_filename)
            except RuntimeError as e:
                self.log("verify fail; ")
                self.log(e)
                continue

            self.log(score)
            self.log(prediction)
            match_score = max(score, match_score)

            if prediction[0]:
                self.log("GOOD")
                flag = True
                auth_uuid = key
                match_score = score
                break

        if flag:
            label = await self.ignite_repository.get('uuid2label', auth_uuid)
            label = "unknown" if label is None else label
            message = str(cache_key) + ",OK " + label + "; score=" + str(match_score)
            message += "; " + transcribed
            # example:
            # b9ae83f2-f0a7-408c-b002-b9d2ba546a22,OK dongjin21; score=tensor([[0.3660]])
        else:
            message = str(cache_key) + ",FAIL; "
            if match_score == torch.tensor([-1.0]):
                message += "voice is not detected; " + transcribed
            # example:
            # b9ae83f2-f0a7-408c-b002-b9d2ba546a21,FAIL;
        return message

    async def on_next_register_pending(self, msg: Message):
        pass

    async def start_async(
            self,
            sleep=0
    ):
        self.consumer.subscribe(['user-pending'], on_assign=reset_offset_beginning)
        while True:
            msg: Optional[Message] = self.consumer.poll(1.0)

            if msg is None:
                continue
            elif msg.error():
                self.log(msg.error())
                continue
            try:
                result = await self.on_next_user_pending(msg)
            except RuntimeError as e:
                self.log(e)

            if result is None:
                self.log(("invalid payload", msg))
                continue
            self.produce(topic='infer', msg=result)
            self.log("SEND OK")

        self.log("Consumer closing..")
        self.consumer.close()
        self.producer.flush()


async def load_kafka_pubsub(bootstrap_servers: Optional[str] = None,
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
        'auto.offset.reset': 'earliest'
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


async def load_speech_models() -> dict[str, Any]:
    models: dict[str, Any] = {
        'sv_model': SpeakerRecognition.from_hparams(source="speechbrain/spkrec-ecapa-voxceleb",
                                                    savedir='pretrained_models/spkrec-ecpa-voxceleb'),
        'vad_model': VAD.from_hparams(source="speechbrain/vad-crdnn-libriparty",
                                      savedir="pretrained_models/vad-crdnn-libriparty"),
        'asr_model': whisper.load_model("tiny.en", in_memory=True)
    }

    # https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb
    models['sv_model'].eval()
    models['vad_model'].eval()
    models['asr_model'].eval()

    return models


async def main(logger):
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

    producer, consumer = await load_kafka_pubsub(bootstrap_servers=bootstrap_servers,
                                                 kafka_user_name=kafka_user_name,
                                                 kafka_user_password=kafka_user_password)
    models = await load_speech_models()
    speech_service = SpeechService(sv_model=models['sv_model'],
                                   vad_model=models['vad_model'],
                                   asr_model=models['asr_model'],
                                   logger=logger)

    verify_service = MainService(
        producer=producer,
        consumer=consumer,
        ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
        speech_service=speech_service,
        logger=logger
    )

    await verify_service.start_async()


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

    asyncio.run(main(main_logger))
