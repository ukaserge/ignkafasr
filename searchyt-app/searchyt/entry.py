#!/usr/bin/env python3

import uuid
import os
import logging
import sys
from typing import Optional 
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException, KafkaError

from .kafka_util import on_commit_completed_print, print_assignment, on_acked_print
from .my_constants import KAFKA_GROUP_ID, SEARCH_REQUEST_TOPIC, ANALYSIS_REQUEST_TOPIC
from .protobuf.search_request_pb2 import SearchRequest
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

    kafka_conf_producer = dict({
        # 'debug': 'broker,topic,msg'
    }, **kafka_conf_common)

    kafka_conf_consumer = dict({
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        # "debug": "consumer,cgrp,topic",
        "on_commit": on_commit_completed_print
    }, **kafka_conf_common)

    return { 'consumer': kafka_conf_consumer, 'producer': kafka_conf_producer }

"""CONSUME AND HANDLING LOOP"""

def _process(
    msg: Message,
    consumer: KafkaConsumer, 
    producer: KafkaProducer
):
    search_request = SearchRequest.FromString(msg.value())
    
    analysis_requests = on_next(search_request)
    for analysis_request in analysis_requests:
        producer.produce(
            topic=ANALYSIS_REQUEST_TOPIC,
            value=analysis_request.SerializeToString(),
            callback=on_acked_print
        )
        producer.poll(3)

    logging.debug("PUBLISH OK : -> analysis.request")
    
    consumer.commit(msg)

    logging.debug("COMMIT OK")

def consume_loop(kafka_conf):
    def get_stream_logger(logger_name: str, logging_level: int = logging.DEBUG):
        logger = logging.getLogger(logger_name)
        logger.setLevel(logging_level)
        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
        logger.addHandler(handler)
        return logger

    logging.info('#%s - Starting consumer group=%s, topic=%s', os.getpid(), kafka_conf['consumer']['group.id'], 'speech.request')
    consumer = KafkaConsumer(kafka_conf['consumer'], logger = get_stream_logger(f'kafka-consumer-{str(uuid.uuid4())}'))
    producer = KafkaProducer(kafka_conf['producer'], logger = get_stream_logger(f'kafka-producer-{str(uuid.uuid4())}'))
    consumer.subscribe([SEARCH_REQUEST_TOPIC], on_assign=print_assignment)

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
                _process(msg, consumer, producer)
        except Exception as e:
            logging.exception('#%s - Worker terminated.', os.getpid())
            logging.error(e)
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
    consume_loop(kafka_conf)
