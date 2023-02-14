#!/usr/bin/env python3

import os
import logging
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List
from queue import Queue

from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException
import threading
from multiprocessing import Process

from .my_constants import KAFKA_GROUP_ID, SPEECH_REQUEST_TOPIC, ANALYSIS_RESULT_SID_TOPIC, YOUTUBE_VIDEO_TABLE, YOUTUBE_VIDEO_SID_TABLE, ONNX_FILENAME
from .ignite_util import IgniteRepository
from .util import get_stream_logger
from .kafka_util import print_assignment, on_acked_print, on_commit_completed_print
from .sr_loop import on_next
from .cassandra_util import CassandraRepository
from .protobuf.speech_request_pb2 import SpeechRequest
from .speaker_recognition import load_onnx_session
from .audio_processing import init_featurizer

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

## Functions for load config ##

def load_kafka_conf():
    kafka_conf_common = {
        'bootstrap.servers': os.environ['BOOTSTRAPSERVERS']
    }
    try:
        kafka_conf_auth = {
            'sasl.mechanism': 'SCRAM-SHA-512',
            'security.protocol': 'SASL_PLAINTEXT',
            'sasl.username': os.environ['KAFKA_USER_NAME'],
            'sasl.password': os.environ['KAFKA_USER_PASSWORD']
        }
        kafka_conf_common = dict(kafka_conf_common, **kafka_conf_auth)
    except KeyError as e:
        logging.debug("WARN: kafka_user_name is None, kafka_user_password is None. ")
    
    kafka_conf_producer = dict({
        #  'queue.buffering.max.ms': 500,
        #  'batch.num.messages': 50,
        #  'acks': 0,
        #  'debug': 'broker,topic,msg',
        #  'max.poll.interval.ms': 420000,
        #  'queue.buffering.max.ms': 36000,
        #  'linger.ms': 36000
    }, **kafka_conf_common)
    
    kafka_conf_consumer = dict({
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        # 'enable.auto.offset.store': False,
        # "partition.assignment.strategy": partition_assignment_strategy,
        # "debug": "consumer,cgrp,topic,fetch",
        "on_commit": on_commit_completed_print
    }, **kafka_conf_common)
    # partition_assignment_strategy = os.environ["CONSUMER_ASSIGN_STRATEGY"]
    
    return { 'consumer': kafka_conf_consumer, 'producer': kafka_conf_producer }

def load_ignite_conf():
    ignite_conf = {
        'IGNITE_SERVICE_NAME': os.environ['IGNITE_SERVICE_NAME'],
        'IGNITE_PORT': int(os.environ['IGNITE_PORT'])
    }
    return ignite_conf

def load_cass_conf():
    cass_conf = {
        'CASS_HOST': os.environ['CASS_HOST'],
        'CASS_USER_NAME': os.environ['CASS_USER_NAME'],
        'CASS_USER_PASSWORD': os.environ['CASS_USER_PASSWORD']
    }
    return cass_conf

## CORE LOGIC ##

def _process(q, consumer, producer, ignite_conf, cass_conf):
    msg = q.get(timeout=60)
    
    logging.info(
        '#%sT%s - Received message: %s',
        os.getpid(), threading.get_ident(), msg.value()
    )
    # consumer.store_offsets(msg)
    
    ignite_repository = IgniteRepository(
        host = ignite_conf['IGNITE_SERVICE_NAME'],
        port = ignite_conf['IGNITE_PORT']
    )
    speech_request = SpeechRequest.FromString(msg.value())
    result, is_success = on_next(ignite_repository, speech_request, session, f)
    del ignite_repository
    
    producer.produce(
        topic = ANALYSIS_RESULT_SID_TOPIC, 
        value = result,
        callback = on_acked_print
    )
    producer.poll(3)
    
    if is_success:
        cassandra_repository = CassandraRepository(
                host = cass_conf['CASS_HOST'],
                username = cass_conf['CASS_USER_NAME'],
                password = cass_conf['CASS_USER_PASSWORD']
        )
        cassandra_repository.connect() 
        cassandra_repository.execute(f"INSERT INTO {YOUTUBE_VIDEO_SID_TABLE} (video_id, bytes_value) VALUES (%s, %s)", (speech_request.videoId, result))
        cassandra_repository.execute(f"UPDATE {YOUTUBE_VIDEO_TABLE} SET is_processed = true WHERE id = %s", (speech_request.videoId, ))
        cassandra_repository.close()
        del cassandra_repository

        logging.debug("analysis result store to cassandra")
    
    q.task_done()
    consumer.commit(msg)

def consume_loop(kafka_conf, ignite_conf, cass_conf):
    # Prepare IgniteRepository
    consumer = KafkaConsumer(kafka_conf['consumer'], logger = get_stream_logger(f'kafka-consumer-{str(uuid.uuid4())}'))
    producer = KafkaProducer(kafka_conf['producer'], logger = get_stream_logger(f'kafka-producer-{str(uuid.uuid4())}'))
    q = Queue(maxsize=4)
    
    # Consume loop
    try:
        consumer.subscribe([SPEECH_REQUEST_TOPIC], on_assign=print_assignment)
        while True:
            msg: Optional[Message] = consumer.poll(60)

            if msg is None:
                logging.debug("Wait...")
                continue
            elif msg.error():
                logging.debug("MSG.ERROR() !!!!")
                raise KafkaException(msg.error())
            
            sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                (msg.topic(), msg.partition(), msg.offset(),
                                str(msg.key())))
            q.put(msg)
            
            t = threading.Thread(target=_process, args=(q, consumer, producer, ignite_conf, cass_conf))
            t.start()
    finally:
        consumer.close()
        producer.flush()

if __name__ == "__main__":
    kafka_conf = load_kafka_conf()
    ignite_conf = load_ignite_conf()
    cass_conf = load_cass_conf()
    session = load_onnx_session(ONNX_FILENAME)
    f = init_featurizer()
   
    workers = []
    while True:
        num_alive = len([w for w in workers if w.is_alive()])
        if num_alive == 4:
            continue
        for _ in range(4-num_alive):
            p = Process(target=consume_loop, daemon=True, args=(kafka_conf, ignite_conf, cass_conf))
            p.start()
            workers.append(p)
            logging.info('Starting worker #%s', p.pid)
