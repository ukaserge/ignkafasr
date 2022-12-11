#!/usr/bin/env python3

import logging
from kafka import KafkaConsumer, KafkaProducer
from speechbrain.pretrained import SpeakerRecognition
from pyignite import Client
import torchaudio
import uuid
import io
import os

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

def main():
    kafka_user_name = os.environ['KAFKA_USER_NAME']
    kafka_user_password = os.environ['KAFKA_USER_PASSWORD']

    logger = get_module_logger(__name__)
    ignite_client = Client()
    with ignite_client.connect('ignite-service', 10800):
        logger.info("OK : connection for ignite")

        auth_cache = ignite_client.get_cache('authCache')
        cache = ignite_client.get_cache('uploadCache')
        
        producer = KafkaProducer(
            bootstrap_servers='my-cluster-kafka-bootstrap:9092',
            sasl_mechanism='SCRAM-SHA-512',
            security_protocol='SASL_PLAINTEXT',
            sasl_plain_username=kafka_user_name,
            sasl_plain_password=kafka_user_password,
        )

        consumer = KafkaConsumer(
            'user-pending',
            bootstrap_servers='my-cluster-kafka-bootstrap:9092',
            group_id='my-group',
            sasl_mechanism='SCRAM-SHA-512',
            security_protocol='SASL_PLAINTEXT',
            sasl_plain_username=kafka_user_name,
            sasl_plain_password=kafka_user_password,
            enable_auto_commit=True,
            auto_offset_reset='latest',
            consumer_timeout_ms=5000000000
        )
    
        logger.info("OK : connection for kafka")
        # logger.info("Connection for Kafka Success")

        verification = SpeakerRecognition.from_hparams(source="speechbrain/spkrec-ecapa-voxceleb")
        name = 'null'
        
        logger.info('OK : Ready to consume')
        for msg in consumer:
            logger.info('consume')
            cache_key = uuid.UUID(msg.value.decode('utf-8'))
            logger.info(cache_key)
            try:
                cand = cache.get(cache_key)
                cand_waveform, cand_sample_rate = torchaudio.load(io.BytesIO(cand))
            except RuntimeError as e:
                logger.info("cache get error. so, ")
                logger.info("BAD")
                producer.send('infer-negative', key=msg.value,value=msg.value)
                continue
            
            logger.info("scan start")
            keys = []
            with auth_cache.scan() as cursor:
                for k, _ in cursor:
                    logger.info('scanning')
                    keys.append(k)
            logger.info("scan end")
            
            if len(keys) == 0:
                logger.info("BAD")
                producer.send('infer-negative', key=msg.value,value=msg.value)
                continue
            keys = list(set(keys))
            logger.info('comparing start')
            t = False
            r = -100
            flag = False
            for k in keys:
                try:
                    auth_value = cache.get(k)
                    if auth_value == '' or auth_value == b'' or auth_value == None:
                        continue
                    auth_waveform, auth_sample_rate = torchaudio.load(io.BytesIO(auth_value))
                except RuntimeError as e:
                    logger.info("cache get error2")
                    continue
                
                score, prediction = verification.verify_batch(cand_waveform, auth_waveform)
                flag = True
                logger.info('compare')
                if prediction[0]:
                    logger.info("GOOD")
                    logger.info(r)
                    t = True
                    break
            logger.info('infer end')  
            if t == True and flag == True:
                producer.send('infer-positive', key=msg.value, value=msg.value)
            else:
                producer.send('infer-negative', key=msg.value,value=msg.value)

if __name__ == "__main__":
    # get_module_logger(__name__).info("LOGGING TEST")
    # logger.info("START")
    main()


