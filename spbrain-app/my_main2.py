#!/usr/bin/env python3

import logging

import torch
from speechbrain.pretrained import SpeakerRecognition
from torch import tensor
from pyignite import AioClient
import torchaudio
import uuid
import io
import os
import asyncio
import wave
# from aiokafka import AIOKafkaConsumer as KafkaConsumer, AIOKafkaProducer as KafkaProducer\
from confluent_kafka\
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, OFFSET_BEGINNING
import time

def get_module_logger(mod_name):
    """
    To use this, do logger = get_module_logger(__name__)
    """
    logger = logging.getLogger(mod_name)
    handler = logging.StreamHandler()
    formatter = logging.Formatter(
        '%(asctime)s [%(name)-12s] %(levelname)-8s %(message)s')
    handler.setFormatter(formatter)
    logger.addHandler(handler)
    logger.setLevel(logging.DEBUG)
    return logger


# https://pytorch.org/audio/stable/tutorials/audio_resampling_tutorial.html#downsample-48-44-1-khz
def resample(
        waveform,
        sample_rate,
        resample_rate,
        lowpass_filter_width=6,
        rolloff=0.99,
        resampling_method="sinc_interpolation",
        beta=None
):
    return torchaudio.functional.resample(
        waveform,
        sample_rate,
        resample_rate,
        lowpass_filter_width=lowpass_filter_width,
        rolloff=rolloff,
        resampling_method=resampling_method,
        beta=beta
    )


def blob_to_waveform_and_sample_rate(blob):
    foo = open("foo.wav",mode="wb")
    foo.write(blob)
    foo.close()

    foo = open("foo.wav", mode="rb")
    cand_waveform, cand_sample_rate = torchaudio.load(foo, format="wav")
    foo.close()

    return cand_waveform, cand_sample_rate

def consume_and_infer(model, cache, auth_cache):
    async def f(consumer: KafkaConsumer, producer: KafkaProducer):
        consume_keys = dict()
        get_module_logger(__name__).info('OK : Ready to consume')

        try:
            while True:
                msg = consumer.poll(1.0)
                if msg is None:
                    # get_module_logger(__name__).info("wait..")
                    continue
                elif msg.error():
                    get_module_logger(__name__).info(msg.error())
                    continue

                get_module_logger(__name__).info('consume')
                cache_key = uuid.UUID(msg.value().decode('utf-8'))
                get_module_logger(__name__).info(cache_key)

                if cache_key in consume_keys:
                    continue

                cand = await cache.get(cache_key)

                if cand is None:
                    get_module_logger(__name__).info("not exists key")
                    continue

                consume_keys[cache_key] = True

                loop = asyncio.get_running_loop()
                try:
                    cand_waveform, cand_sample_rate = await loop.run_in_executor(
                        None,
                        lambda: blob_to_waveform_and_sample_rate(cand)
                    )
                    cand_waveform = await loop.run_in_executor(
                         None,
                         lambda: resample(cand_waveform, cand_sample_rate, 16000)
                    )
                    get_module_logger(__name__).info(cand_waveform.shape)
                except RuntimeError as e:
                    get_module_logger(__name__).info(e)
                    get_module_logger(__name__).info("BAD")
                    producer.produce(topic='infer-negative', value=msg.value())
                    producer.poll(0)
                    # producer.flush()
                    consumer.commit()
                    continue

                
                get_module_logger(__name__).info("scan start")

                keys = []
                async with auth_cache.scan() as cursor:
                    async for k, _ in cursor:
                        get_module_logger(__name__).info('scanning')
                        keys.append(k)

                t = False
                for k in keys:
                    if k == '' or k == b'' or k is None:
                        continue

                    auth_data = await cache.get(k)
                    if auth_data is None:
                        continue

                    try:
                        auth_waveform, auth_sample_rate = await loop.run_in_executor(
                            None,
                            lambda: blob_to_waveform_and_sample_rate(auth_data)
                        )

                        get_module_logger(__name__).info(auth_waveform.shape)
                        # loop = asyncio.get_running_loop()
                        auth_waveform = await loop.run_in_executor(
                             None,
                             lambda: resample(auth_waveform, auth_sample_rate, 16000)
                        )

                        score, prediction = await loop.run_in_executor(
                            None,
                            lambda: model.verify_batch(cand_waveform, auth_waveform, threshold=0.5)
                        )
                    except RuntimeError as e:
                        get_module_logger(__name__).info(e)
                        continue

                    get_module_logger(__name__).info(score)
                    get_module_logger(__name__).info(prediction)

                    if prediction[0] and score[0][0].item() > 0.3:
                        get_module_logger(__name__).info("GOOD")
                        t = True
                        break
                if t:
                    get_module_logger(__name__).info("SEND_AND_WAIT")
                    producer.produce(topic='infer-positive', value=msg.value())
                    get_module_logger(__name__).info("SEND OK")
                    producer.poll(0)
                    consumer.commit()
                else:
                    get_module_logger(__name__).info("BAD")
                    producer.produce(topic='infer-negative', value=msg.value())
                    producer.poll(0)
                    consumer.commit()
        finally:
            consumer.close()
            producer.flush()

    return f


async def connect_ignite(ignite_host, ignite_port, do_something):
    ignite_client = AioClient()
    async with ignite_client.connect(ignite_host, ignite_port):
        get_module_logger(__name__).info("OK : connection for ignite")
        cache = await ignite_client.get_cache('uploadCache')
        auth_cache = await ignite_client.get_cache('authCache')
        await do_something(cache, auth_cache)

def reset_offset(consumer, partitions):
    for p in partitions:
        p.offset = OFFSET_BEGINNING
    consumer.assign(partitions)

async def connect_kafka(
        bootstrap_servers,
        kafka_user_name,
        kafka_user_password,
        do_something
):
    producer = KafkaProducer({
        'bootstrap.servers': bootstrap_servers,
        'sasl.mechanism': 'SCRAM-SHA-512',
        'security.protocol': 'SASL_PLAINTEXT',
        'sasl.username': kafka_user_name,       # Confluent specific field
        'sasl.password': kafka_user_password,   # Confluent specific field
        'queue.buffering.max.ms': 500,
        'batch.num.messages': 50,
    })

    consumer = KafkaConsumer({
        'bootstrap.servers': bootstrap_servers,
        'sasl.mechanism': 'SCRAM-SHA-512',
        'security.protocol': 'SASL_PLAINTEXT',
        'sasl.username': kafka_user_name,       # Confluent specific field
        'sasl.password': kafka_user_password,   # Confluent specific field
        # 'max.poll.interval.ms': 60000,
        'enable.auto.commit': True,
        'group.id': 'my-group',
        'auto.offset.reset': 'earliest'
    })

    get_module_logger(__name__).info("OK : connection for kafka")
    consumer.subscribe(['user-pending'], on_assign=reset_offset)
    get_module_logger(__name__).info("START : consumer.start()")
    get_module_logger(__name__).info("DO : do_something(consumer, producer)")

    while True:
        time.sleep(1)
        await do_something(consumer, producer)


async def main():
    bootstrap_servers = os.environ['BOOTSTRAPSERVERS']
    kafka_user_name = os.environ['KAFKA_USER_NAME']
    kafka_user_password = os.environ['KAFKA_USER_PASSWORD']
    ignite_host = 'ignite-service'
    ignite_port = 10800
    model = SpeakerRecognition.from_hparams(source="speechbrain/spkrec-ecapa-voxceleb")

    async def f(cache, auth_cache):
        await connect_kafka(
            bootstrap_servers,
            kafka_user_name,
            kafka_user_password,
            consume_and_infer(model, cache, auth_cache)
        )

    await connect_ignite(
        ignite_host,
        ignite_port,
        f
    )

if __name__ == "__main__":
    # model = SpeakerRecognition.from_hparams(source="speechbrain/spkrec-ecapa-voxceleb")

    asyncio.run(main())
