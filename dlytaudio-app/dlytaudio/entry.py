#!/usr/bin/env python3

import os
import logging
import sys
import uuid
from typing import Optional, Dict
import threading
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException, KafkaError, TopicPartition
from google.cloud import datastore
from google.oauth2.service_account import Credentials

from .protobuf.analysis_request_pb2 import AnalysisRequest
from .my_constants import KAFKA_GROUP_ID, ANALYSIS_REQUEST_TOPIC
from .kafka_util import print_assignment, on_acked_print, on_commit_completed_print
from .core import on_next

"""LOADER FUNCTIONS"""

def load_kafka_conf():
    kafka_conf_common = {
        'bootstrap.servers': os.environ['BOOTSTRAPSERVERS']
    }
    try:
        kafka_conf_auth = {
            'sasl.mechanism': os.environ['KAFKA_SASL_MECHANISM'],
            'security.protocol': os.environ['KAFKA_SECURITY_PROTOCOL'],
            'sasl.username': os.environ['KAFKA_USER_NAME'],
            'sasl.password': os.environ['KAFKA_USER_PASSWORD']
        }

        kafka_conf_common = dict(kafka_conf_common, **kafka_conf_auth)
    except KeyError as e:
        logging.debug("WARN: kafka_user_name is None, kafka_user_password is None. ")
    
    kafka_conf_producer = dict({}, **kafka_conf_common)
    kafka_conf_consumer = dict({
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        "on_commit": on_commit_completed_print
    }, **kafka_conf_common)
    
    return { 'consumer': kafka_conf_consumer, 'producer': kafka_conf_producer }

def load_datastore_client_loader():
    cred_file = os.environ['BQ_CREDENTIALS_JSON_FILE']
    def client_loader():
        credentials = Credentials.from_service_account_file(cred_file)
        client = datastore.Client(credentials=credentials)
        return client
    return client_loader

"""CONSUME AND HANDLING LOOP"""

def _process(
    msg: Message,
    consumer: KafkaConsumer,
    producer: KafkaProducer,
    client_loader
):
    logging.info(
        '#%sT%s - Received message: %s',
        
        os.getpid(), threading.get_ident(), msg.value()
    )
    
    analysis_request = AnalysisRequest.FromString(msg.value())

    on_next(analysis_request, client_loader, producer)
    
    consumer.commit(msg)

def consume_loop(kafka_conf, client_loader):
    def get_stream_logger(logger_name: str, logging_level: int = logging.DEBUG):
        logger = logging.getLogger(logger_name)
        logger.setLevel(logging_level)
        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
        logger.addHandler(handler)
        return logger

    logging.info('#%s - Starting consumer group=%s, topic=%s', os.getpid(), kafka_conf['consumer']['group.id'], 'analysis.request')
    
    consumer = KafkaConsumer(kafka_conf['consumer'], logger = get_stream_logger(f'kafka-consumer-{str(uuid.uuid4())}'))
    producer = KafkaProducer(kafka_conf['producer'], logger = get_stream_logger(f'kafka-producer-{str(uuid.uuid4())}'))
    consumer.subscribe([ANALYSIS_REQUEST_TOPIC], on_assign=print_assignment)
    
    while True:
        logging.info('#%s - Waiting for message...', os.getpid())
        try:
            msg: Optional[Message] = consumer.poll(1.0)
            if msg is None:
                continue
            
            if msg.error():
                logging.error('#%s - Consumer error: %s', os.getpid(), msg.error())
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    sys.stderr.write("%s %s [%d] reached end at of offset %d\n" % 
                                     (msg.topic(), msg.partition(), msg.offset()))
                elif msg.error():
                    logging.error("Kafka Exception")
                    logging.error(msg.error())

                    raise KafkaException(msg.error())
            else:
                consumer.pause([TopicPartition(msg.topic(), msg.partition(), msg.offset())])
                _process(msg, consumer, producer, client_loader)
                consumer.resume([TopicPartition(msg.topic(), msg.partition(), msg.offset())])
        except Exception as e:
            logging.exception('#%s - Worker terminated.', os.getpid())
            logging.error(e)
            consumer.close()
            producer.flush()
            break

"""MAIN"""
    
if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
        level=logging.DEBUG,
        datefmt="%H:%M:%S",
        stream=sys.stdout,
    )
    
    kafka_conf = load_kafka_conf()
    client_loader = load_datastore_client_loader() 

    consume_loop(kafka_conf, client_loader)

