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
from confluent_kafka\
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, OFFSET_BEGINNING
import time
from speechbrain.pretrained import SpectralMaskEnhancement

import sys
logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stderr,
)
logger = logging.getLogger("areq")
logging.getLogger("chardet.charsetprober").disabled = True

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


def blob_to_waveform_and_sample_rate(blob, enhancer):
    foo = open("foo.wav",mode="wb")
    foo.write(blob)
    foo.close()

    enhancement("foo.wav", output_filename="e-foo.wav", enhancer=enhancer)

    # foo = open("qoo.wav", mode="rb")
    cand_waveform, cand_sample_rate = torchaudio.load("e-foo.wav")
    # foo.close()

    return cand_waveform, cand_sample_rate

def enhancement(filepath, output_filename=None, enhancer=None):
    if enhancer is None:
        assert False

    noisy = enhancer.load_audio(filepath).unsqueeze(0)
    enhanced = enhancer.enhance_batch(noisy, lengths=torch.tensor([1.0]))

    if output_filename is not None:
        torchaudio.save(output_filename, enhanced.cpu(), 16000)

    return enhanced

def consume_and_infer(model, cache, auth_cache):
    async def auth_users_key_list():
        keys = []
        logger.info("scan start")
        async with auth_cache.scan() as cursor:
            async for k, _ in cursor:
                logger.info('scanning')
                keys.append(k)
        return keys

    async def loop_consume_and_infer(consumer, producer):
        consume_keys = dict()

        # https://huggingface.co/speechbrain/metricgan-plus-voicebank
        enhancer = SpectralMaskEnhancement.from_hparams(source="speechbrain/metricgan-plus-voicebank")
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            elif msg.error():
                logger.info(msg.error())
                continue

            logger.info('consume')

            cache_key = uuid.UUID(msg.value().decode('utf-8'))

            logger.info(cache_key)

            if cache_key in consume_keys:
                continue

            cand = await cache.get(cache_key)

            if cand is None:
                logger.info("not exists key")
                continue

            consume_keys[cache_key] = True

            loop = asyncio.get_running_loop()
            try:
                cand_waveform, cand_sample_rate = await loop.run_in_executor(
                    None,
                    lambda: blob_to_waveform_and_sample_rate(cand, enhancer=enhancer)
                )
                # Suppose sample rates of inputs are 16000
                # cand_waveform = await loop.run_in_executor(
                #      None,
                #      lambda: resample(cand_waveform, cand_sample_rate, 16000)
                # )

                logger.info(cand_waveform.shape)
            except RuntimeError as e:
                logger.info(e)
                logger.info("BAD")

                producer.produce(topic='infer-negative', value=msg.value())
                producer.poll(0)
                # producer.flush()
                consumer.commit()
                continue

            keys = await auth_users_key_list()

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
                        lambda: blob_to_waveform_and_sample_rate(auth_data, enhancer=enhancer)
                    )

                    logger.info(auth_waveform.shape)

                    score, prediction = await loop.run_in_executor(
                        None,
                        lambda: model.verify_batch(cand_waveform, auth_waveform, threshold=0.4)
                    )
                except RuntimeError as e:
                    logger.info(e)
                    continue

                logger.info(score)
                logger.info(prediction)

                if prediction[0]:
                    logger.info("GOOD")
                    t = True
                    break

            logger.info("SEND_AND_WAIT")

            destination_topic = 'infer-positive' if t else 'infer-negative'
            producer.produce(topic=destination_topic, value=msg.value())

            logger.info("SEND OK")

            producer.poll(0)
            consumer.commit()

    async def f(consumer: KafkaConsumer, producer: KafkaProducer):
        logger.info('OK : Ready to consume')

        try:
            await loop_consume_and_infer(consumer, producer)
        finally:
            consumer.close()
            producer.flush()

    return f


async def connect_ignite(ignite_host, ignite_port, do_something):
    ignite_client = AioClient()
    async with ignite_client.connect(ignite_host, ignite_port):
        logger.info("OK : connection for ignite")
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

    logger.info("OK : connection for kafka")
    consumer.subscribe(['user-pending'], on_assign=reset_offset)
    logger.info("START : consumer.start()")
    logger.info("DO : do_something(consumer, producer)")

    while True:
        time.sleep(0.1)
        await do_something(consumer, producer)


async def main():
    bootstrap_servers = os.environ['BOOTSTRAPSERVERS']
    kafka_user_name = os.environ['KAFKA_USER_NAME']
    kafka_user_password = os.environ['KAFKA_USER_PASSWORD']
    ignite_host = 'ignite-service'
    ignite_port = 10800

    # https://huggingface.co/speechbrain/spkrec-ecapa-voxceleb
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
    asyncio.run(main())
