from typing import Dict
import uuid
import os
import sys
import logging
import threading

from google.oauth2.service_account import Credentials
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, KafkaException, KafkaError, TopicPartition

from .kafka_util import print_assignment, on_acked_print, on_commit_completed_print
from .my_constants import ANALYSIS_RESULT_SID_TOPIC, KAFKA_GROUP_ID
from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .core import on_next

"""LOADER FUNCTIONS"""

def load_bq_conf():
    cred_file = os.environ["BQ_CREDENTIALS_JSON_FILE"]
    dataset_name = os.environ["BQ_DATASET_NAME"]
    table_name = os.environ["BQ_TABLE_NAME"]
    
    def credentials_loader():
        CRED_SCOPE_1 = "https://www.googleapis.com/auth/cloud-platform"
        return Credentials.from_service_account_file(cred_file, scopes=[CRED_SCOPE_1])

    return { "credentials_loader": credentials_loader, "dataset_name": dataset_name, "table_name": table_name }

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
    
    kafka_conf_consumer = dict({
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        # "debug": "consumer,cgrp,topic",
        "on_commit": on_commit_completed_print
    }, **kafka_conf_common)
    
    return { 'consumer': kafka_conf_consumer }


"""CONSUME AND HANDLING LOOP"""

def _process(
    msg: Message, 
    consumer: KafkaConsumer, 
    bq_conf: Dict
): 
    logging.info(
        '#%sT%s - Received message: %s',
        os.getpid(), threading.get_ident(), msg.value()
    )
    
    analysis_result_sid = AnalysisResultSid.FromString(msg.value())

    on_next(analysis_result_sid, bq_conf) 
    
    consumer.commit(msg)

def consume_loop(kafka_conf, bq_conf): 
    def get_stream_logger(logger_name: str, logging_level: int = logging.DEBUG):
        logger = logging.getLogger(logger_name)
        logger.setLevel(logging_level)
        handler = logging.StreamHandler()
        handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
        logger.addHandler(handler)
        return logger
    
    logging.info('#%s - Starting consumer group=%s, topic=%s', os.getpid(), kafka_conf['consumer']['group.id'], 'analysis.result.sid')
    
    consumer = KafkaConsumer(kafka_conf['consumer'], logger = get_stream_logger(f'kafka-consumer-{str(uuid.uuid4())}'))
    consumer.subscribe([ANALYSIS_RESULT_SID_TOPIC], on_assign=print_assignment)
    
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
                _process(msg = msg, consumer = consumer, bq_conf = bq_conf)
        except Exception as e:
            logging.exception('#%s - Worker terminated.', os.getpid())
            logging.error(e)
            consumer.close()
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
    bq_conf = load_bq_conf()

    consume_loop(kafka_conf = kafka_conf, bq_conf = bq_conf)
