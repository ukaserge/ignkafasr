#!/usr/bin/env python3

import os
import logging
import sys
from typing import Optional, Tuple, Any, Dict, List
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException

from .ignite import IgniteRepository
from .util import get_stream_logger
from .kafka import get_kafka_config
from .scrap_loop import ScrapMain
from .cassandra import CassandraRepository

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

def main():
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
    
    cassandra_host = os.environ['CASS_HOST']
    cassandra_username = os.environ['CASS_USER_NAME']
    cassandra_password = os.environ['CASS_USER_PASSWORD']

    # Load kafka producer, consumer
    group_id = 'cc-group' 
    kafka_config_dict = get_kafka_config(bootstrap_servers=bootstrap_servers, group_id=group_id, kafka_user_name=kafka_user_name, kafka_user_password=kafka_user_password)
    producer = KafkaProducer(kafka_config_dict['producer'], logger = get_stream_logger('kafka-producer'))
    consumer = KafkaConsumer(kafka_config_dict['consumer'], logger = get_stream_logger('kafka-consumer'))
    
    main_obj = ScrapMain(
        producer=producer,
        consumer=consumer,
        ignite_repository=IgniteRepository(host=ignite_host, port=ignite_port),
        cassandra_repository=CassandraRepository(host=cassandra_host, username=cassandra_username, password=cassandra_password)
    )

    # Start...
    main_obj.run_loop()

if __name__ == "__main__":
    main()
