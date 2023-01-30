#!/usr/bin/env python3

import os
import logging
import sys
# import uuid
from typing import Optional, Tuple, Any, Dict, List

from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException

from .speech_service import SpeechService
from .ignite import IgniteRepository
from .util import get_stream_logger
from .kafka import get_kafka_config
from .transcribe_loop import TranscribeMain
from .verify_loop import VerifyMain

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

def main(mode: str):
    # Load Environment Variables
    try:
        bootstrap_servers = os.environ['BOOTSTRAPSERVERS']
        ignite_host = os.environ['IGNITE_SERVICE_NAME']
        ignite_port = int(os.environ['IGNITE_PORT'])
    except KeyError as e:
        logging.debug(e)
        raise RuntimeError("Fail to load environment variables")
    try:
        kafka_user_name = os.environ['KAFKA_USER_NAME']
        kafka_user_password = os.environ['KAFKA_USER_PASSWORD']
    except KeyError as e:
        logging.debug("WARN: kafka_user_name is None, kafka_user_password is None. ")
        kafka_user_name = None
        kafka_user_password = None
    # partition_assignment_strategy = os.environ["CONSUMER_ASSIGN_STRATEGY"]
    
    # Load speech models
    if mode == 'verify':
        from speechbrain.pretrained import EncoderClassifier
        model = EncoderClassifier.from_hparams(
            source="/app/asrspeech/spkrec-ecapa-voxceleb",
            savedir="saved"
         )
        model.eval()
        model.zero_grad(set_to_none=True)
        speech_service = SpeechService(embedding_model=model, transcript_model=None)
    elif mode == 'transcribe':
        import whisper
        model = whisper.load_model("tiny.en", device='cpu', in_memory=True)
        model.eval()
        model.zero_grad(set_to_none=True)
        speech_service = SpeechService(embedding_model=None, transcript_model=model)
    else:
        raise RuntimeError("mode == verify || mode == transcribe")
        
    # Load kafka producer, consumer
    group_id = 'uu-group' if mode == 'verify' else 'tt-group'
    kafka_config_dict = get_kafka_config(bootstrap_servers=bootstrap_servers, group_id=group_id, kafka_user_name=kafka_user_name, kafka_user_password=kafka_user_password)
    producer = KafkaProducer(kafka_config_dict['producer'], logger = get_stream_logger('kafka-producer'))
    consumer = KafkaConsumer(kafka_config_dict['consumer'], logger = get_stream_logger('kafka-consumer'))
    
    if mode == 'verify':
        main_obj = VerifyMain(
            producer=producer,
            consumer=consumer,
            ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
            speech_service=speech_service
        )
    elif mode == 'transcribe':
        main_obj = TranscribeMain(
            producer=producer,
            consumer=consumer,
            ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
            speech_service=speech_service
        )
    
    # Start...
    main_obj.run_loop()

if __name__ == "__main__":
    main(mode = sys.argv[1])
