#!/usr/bin/env python3

import logging
from speechbrain.pretrained import SpeakerRecognition

from pyignite import AioClient
import torchaudio
import uuid
import io
import os
import asyncio
from aiokafka import AIOKafkaConsumer as KafkaConsumer, AIOKafkaProducer as KafkaProducer


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
        beta=beta,
    )


def consume_and_infer(model, cache, auth_cache):
    async def f(consumer, producer):
        consume_keys = dict()
        get_module_logger(__name__).info('OK : Ready to consume')

        try:
            async for msg in consumer:
                get_module_logger(__name__).info('consume')
                cache_key = uuid.UUID(msg.value.decode('utf-8'))
                get_module_logger(__name__).info(cache_key)

                if cache_key in consume_keys:
                    continue

                cand = await cache.get(cache_key)

                if cand is None:
                    get_module_logger(__name__).info("not exists key")
                    continue

                consume_keys[cache_key] = True

                assert (type(cand) == bytes)

                loop = asyncio.get_running_loop()
                cand_waveform, cand_sample_rate = await loop.run_in_executor(
                    None,
                    lambda: torchaudio.load(io.BytesIO(cand))
                )
                cand_waveform = await loop.run_in_executor(
                    None,
                    lambda: resample(cand_waveform, cand_sample_rate, 12800)
                )

                get_module_logger(__name__).info("scan start")
                t = False
                async with auth_cache.scan() as cursor:
                    async for k, _ in cursor:
                        get_module_logger(__name__).info('scanning')
                        if k == '' or k == b'' or k is None:
                            continue

                        auth_data = await cache.get(k)
                        if auth_data is None:
                            continue

                        loop = asyncio.get_running_loop()
                        auth_waveform, auth_sample_rate = await loop.run_in_executor(
                            None,
                            lambda: torchaudio.load(io.BytesIO(auth_data))
                        )
                        auth_waveform = await loop.run_in_executor(
                            None,
                            lambda: resample(auth_waveform, auth_sample_rate, 12800)
                        )
                        score, prediction = await loop.run_in_executor(
                            None,
                            lambda: model.verify_batch(cand_waveform, auth_waveform)
                        )

                        get_module_logger(__name__).info(score)

                        if prediction[0] and score[0][0].item() > 0.8:
                            get_module_logger(__name__).info("GOOD")
                            t = True
                            raise StopAsyncIteration
                    if t:
                        get_module_logger(__name__).info("SEND_AND_WAIT")
                        await producer.send_and_wait('infer-positive', msg.value)
                        get_module_logger(__name__).info("SEND OK")
                    else:
                        get_module_logger(__name__).info("BAD")
                        await producer.send_and_wait('infer-negative', msg.value)
        except RuntimeError as e:
            get_module_logger(__name__).info(e)
            # raise e
        finally:
            get_module_logger(__name__).info("CONSUMER STOP")
            await consumer.stop()

    return f


async def connect_ignite(ignite_host, ignite_port, do_something):
    ignite_client = AioClient()
    async with ignite_client.connect(ignite_host, ignite_port):
        get_module_logger(__name__).info("OK : connection for ignite")
        cache = await ignite_client.get_cache('uploadCache')
        auth_cache = await ignite_client.get_cache('authCache')
        await do_something(cache, auth_cache)


async def connect_kafka(
        bootstrap_servers,
        kafka_user_name,
        kafka_user_password,
        do_something
):
    producer = KafkaProducer(
        bootstrap_servers=bootstrap_servers,
        sasl_mechanism='SCRAM-SHA-512',
        security_protocol='SASL_PLAINTEXT',
        sasl_plain_username=kafka_user_name,
        sasl_plain_password=kafka_user_password,
    )

    consumer = KafkaConsumer(
        'user-pending',
        bootstrap_servers=bootstrap_servers,
        group_id='my-group',
        sasl_mechanism='SCRAM-SHA-512',
        security_protocol='SASL_PLAINTEXT',
        sasl_plain_username=kafka_user_name,
        sasl_plain_password=kafka_user_password,
        enable_auto_commit=True,
        auto_offset_reset='earliest',
        max_poll_interval_ms=60000
    )
    get_module_logger(__name__).info("OK : connection for kafka")

    get_module_logger(__name__).info("START : consumer.start()")
    await consumer.start()

    get_module_logger(__name__).info("DO : do_something(consumer, producer)")
    await do_something(consumer, producer)


async def main():
    bootstrap_servers = 'my-cluster-kafka-bootstrap:9092'
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
