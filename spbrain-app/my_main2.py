#!/usr/bin/env python3

import logging

import torch
from speechbrain.processing.speech_augmentation import DropChunk, DropFreq
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
# from speechbrain.pretrained import SpectralMaskEnhancement

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
        # resampling_method="sinc_interpolation",
        resampling_method="kaiser_window",
        beta=None
):
    tf = torchaudio.transforms.Resample(
        orig_freq=sample_rate, new_freq=resample_rate,
        lowpass_filter_width=lowpass_filter_width,
        rolloff=rolloff,
        resampling_method=resampling_method,
        beta=beta
    )
    waveform = tf(waveform)
    return waveform

def blob_to_waveform_and_sample_rate(blob):
    foo = open("foo.wav",mode="wb")
    foo.write(blob)
    foo.close()

    # enhancement("foo.wav", output_filename="e-foo.wav", enhancer=enhancer)
    # foo = open("qoo.wav", mode="rb")
    cand_waveform, cand_sample_rate = torchaudio.load("foo.wav")
    # foo.close()

    return cand_waveform, cand_sample_rate
"""
def enhancement(filepath, output_filename=None, enhancer=None):
    if enhancer is None:
        assert False

    noisy = enhancer.load_audio(filepath).unsqueeze(0)
    enhanced = enhancer.enhance_batch(noisy, lengths=torch.tensor([1.0]))

    if output_filename is not None:
        torchaudio.save(output_filename, enhanced.cpu(), 16000)

    return enhanced
"""

# /speechbrain/pretrained/interfaces.py : separate_file(...)
def enhancement(wf, fs=16000, enh_model=None):
    wf = wf.unsqueeze(0)
    if fs != 8000:
        # wf = wf.mean(dim=0, keepdim=True)
        wf = resample(wf, fs, 8000)

    est_sources = enh_model.separate_batch(wf)
    est_sources = (
            est_sources / est_sources.abs().max(dim=1, keepdim=True)[0]
    )
    return resample(est_sources.squeeze(), 8000, fs)

def time_dropout(wf):
    dropper = DropChunk(drop_length_low=100, drop_length_high=1000, drop_count_low=1, drop_count_high=10)
    length = torch.ones(1)
    signal = wf.unsqueeze(0)
    dropped_signal = dropper(signal, length)
    return dropped_signal

def freq_dropout(wf):
    dropper = DropFreq(
        drop_count_low=1,
        drop_count_high=8
    )
    signal = wf.unsqueeze(0)
    dropped_signal = dropper(signal)
    return dropped_signal

def noise_corruption(wf):
    corrupter = EnvCorrupt(openrir_folder='.')
    noise_rev = corrupter(wf.unsqueeze(0), torch.ones(1))

    return noise_rev.squeeze(0)

def noise_reverb(wf):
    reverb = AddReverb('rirs.csv', rir_scale_factor=1.0)
    reverbed = reverb(wf.unsqueeze(0), torch.ones(1))
    return reverbed

def clipping(wf):
    clipper = DoClip(
        # clip_low=0.7, clip_high=0.7
    )
    clipped_signal = clipper(wf.unsqueeze(0))
    return clipped_signal

def run_vad(filepath):
    from speechbrain.pretrained import VAD
    VAD = VAD.from_hparams(
        source="speechbrain/vad-crdnn-libriparty",
        savedir="pretrained_models/vad-crdnn-libriparty"
    )

    # 1- Let's compute frame-level posteriors first
    audio_file = filepath
    prob_chunks = VAD.get_speech_prob_file(
        audio_file,
        small_chunk_size=1,
        large_chunk_size=10
    )
    # print(prob_chunks)
    # 2- Let's apply a threshold on top of the posteriors
    prob_th = VAD.apply_threshold(
        prob_chunks,
        # activation_th=0.8,
        # deactivation_th=0.4
        # activation_th=0.7,
        # deactivation_th=0.4
    ).float()

    # 3- Let's now derive the candidate speech segments
    boundaries = VAD.get_boundaries(prob_th)

    # 4- Apply energy VAD within each candidate speech segment (optional)
    # boundaries = VAD.energy_VAD(
    #     audio_file, boundaries,
    #     activation_th=0.7,
    #     deactivation_th=0.4
    # )

    # print(boundaries)

    # 5- Merge segments that are too close
    boundaries = VAD.merge_close_segments(boundaries, close_th=0.3)

    # print(boundaries)

    # 6- Remove segments that are too short
    # boundaries = VAD.remove_short_segments(boundaries, len_th=0.3)

    # print(boundaries)

    # 7- Double-check speech segments (optional).
    # boundaries = VAD.double_check_speech_segments(boundaries, audio_file,  speech_th=0.25)

    # print(boundaries)

    VAD.save_boundaries(boundaries)

    return boundaries

# fd if voice active
def verify(signal1, signal2, model=None):
    assert model is not None

    seg1 = wf_to_vad_segments(signal1.squeeze())
    seg2 = wf_to_vad_segments(signal2.squeeze())
    
    if len(seg1) == 0 and len(seg2) == 0:
        print("signal1,2 voice not detected")
        return torch.tensor([[-1.0]]), torch.tensor([[False]])
    elif len(seg1) == 0:
        print("signal1 voice not detected")
        return torch.tensor([[-1.0]]), torch.tensor([[False]])
    elif len(seg2) == 0:
        print("signal2 voice not detected")
        return torch.tensor([[-1.0]]), torch.tensor([[False]])
    else:
        result = model.verify_batch(freq_dropout(signal1.squeeze()).squeeze(), freq_dropout(signal2.squeeze()).squeeze())
        print(result)
        return result 
    
def wf_to_vad_segments(wf):
    torchaudio.save("foo.wav", wf.unsqueeze(0), 16000)
    from speechbrain.pretrained import VAD
    VAD = VAD.from_hparams(
        source="speechbrain/vad-crdnn-libriparty",
        savedir="pretrained_models/vad-crdnn-libriparty"
    )
    return VAD.get_segments(boundaries=run_vad("foo.wav"), audio_file="foo.wav")

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
        # enhancer = SpectralMaskEnhancement.from_hparams(source="speechbrain/metricgan-plus-voicebank")
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
                    lambda: blob_to_waveform_and_sample_rate(cand)
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
                        lambda: blob_to_waveform_and_sample_rate(auth_data)
                    )

                    logger.info(auth_waveform.shape)
                    
                    score, prediction = await loop.run_in_executor(
                        None,
                        lambda: verify(cand_waveform, auth_waveform, model)
                        # model.verify_batch(cand_waveform, auth_waveform, threshold=0.4)
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
