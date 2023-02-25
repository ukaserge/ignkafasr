#!/usr/bin/env python3

import os
import logging
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List
from queue import Queue
import torch
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException, KafkaError, TopicPartition
import threading
from multiprocessing import Process
import time
import onnxruntime

from .my_constants import KAFKA_GROUP_ID, SPEECH_REQUEST_TOPIC, ANALYSIS_RESULT_SID_TOPIC, SR_ONNX_FILENAME, SR_CONF, VAD_ONNX_FILENAME, VAD_CONF
from .kafka_util import print_assignment, on_acked_print, on_commit_completed_print
from .sr_loop import on_next
from .protobuf.speech_request_pb2 import SpeechRequest
from .audio_processing import load_featurizer
from .ignite_util import IgniteTemplate

def load_onnx_session(onnx_filename):
    return onnxruntime.InferenceSession(onnx_filename)

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

def load_ignite_conf():
    ignite_conf = {
        'IGNITE_SERVICE_NAME': os.environ['IGNITE_SERVICE_NAME'],
        'IGNITE_PORT': int(os.environ['IGNITE_PORT'])
    }
    return ignite_conf

def _process(msg, consumer, producer, ignite_conf, sr_session, sr_f, vad_session, vad_f):
    # msg = q.get(timeout=60)
    
    logging.info(
        '#%sT%s - Received message: %s',
        os.getpid(), threading.get_ident(), msg.value()
    )
    
    speech_request = SpeechRequest.FromString(msg.value())
    ignite_template = IgniteTemplate(ignite_conf['IGNITE_SERVICE_NAME'], ignite_conf['IGNITE_PORT'])

    result, is_success = on_next(ignite_template, speech_request, sr_session, sr_f, vad_session, vad_f)
    
    producer.produce(
        topic = ANALYSIS_RESULT_SID_TOPIC, 
        value = result,
        callback = on_acked_print
    )
    producer.poll(3)
    # q.task_done()
    consumer.commit(msg)

def consume_loop(kafka_conf, ignite_conf):
    def get_stream_logger(logger_name: str, logging_level: int = logging.DEBUG):
        logger = logging.getLogger(logger_name)
        logger.setLevel(logging_level)
        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
        logger.addHandler(handler)
        return logger
    
    sr_session = load_onnx_session(SR_ONNX_FILENAME)
    sr_f = load_featurizer(SR_CONF)
    
    vad_session = load_onnx_session(VAD_ONNX_FILENAME)
    vad_f = load_featurizer(VAD_CONF)

    logging.info('#%s - Starting consumer group=%s, topic=%s', os.getpid(), kafka_conf['consumer']['group.id'], 'speech.request')
    
    consumer = KafkaConsumer(kafka_conf['consumer'], logger = get_stream_logger(f'kafka-consumer-{str(uuid.uuid4())}'))
    producer = KafkaProducer(kafka_conf['producer'], logger = get_stream_logger(f'kafka-producer-{str(uuid.uuid4())}'))
    consumer.subscribe([SPEECH_REQUEST_TOPIC], on_assign=print_assignment)
    # q = Queue(maxsize=1)
    
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
                # t = Process(target=_process, daemon=True, args=(q, consumer, producer, ignite_conf))
                # t.start()
                _process(
                        msg = msg, 
                        consumer = consumer, 
                        producer = producer, 
                        ignite_conf = ignite_conf, 
                        sr_session = sr_session, 
                        sr_f = sr_f, 
                        vad_session = vad_session, 
                        vad_f = vad_f
                    )
                consumer.resume([TopicPartition(msg.topic(), msg.partition(), msg.offset())])
        except Exception as e:
            logging.exception('#%s - Worker terminated.', os.getpid())
            logging.error(e)
            consumer.close()
            producer.flush()
            break

if __name__ == "__main__":    
    logging.basicConfig(
        format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
        level=logging.DEBUG,
        datefmt="%H:%M:%S",
        stream=sys.stdout,
    )
    torch.set_grad_enabled(False)
    
    kafka_conf = load_kafka_conf()
    ignite_conf = load_ignite_conf()
   
    consume_loop(kafka_conf, ignite_conf)

#cassandra_repository.connect() 
#cassandra_repository.execute(f"INSERT INTO {YOUTUBE_VIDEO_SID_TABLE} (video_id, bytes_value) VALUES (%s, %s)", (speech_request.videoId, result))
#cassandra_repository.execute(f"UPDATE {YOUTUBE_VIDEO_TABLE} SET is_processed = true WHERE id = %s", (speech_request.videoId, ))
#cassandra_repository.close()

