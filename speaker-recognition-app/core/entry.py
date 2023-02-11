#!/usr/bin/env python3

import os
import logging
import sys
# import uuid
from typing import Optional, Tuple, Any, Dict, List

from speechbrain.pretrained import EncoderClassifier
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException

from .ignite_util import IgniteRepository
from .util import get_stream_logger
from .kafka_util import get_kafka_config
from .speaker_recognition import SrService
from .sr_loop import SrMain
from .cassandra_util import CassandraRepository

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# KAFKA
KAFKA_GROUP_ID = 'sr-group'

# SPEECHBRAIN
SR_MODEL_SOURCE = "/app/core/spkrec-ecapa-voxceleb"
SR_SAVEDIR = "saved"

def main():
    # Load Environment Variables
    try:
        bootstrap_servers = os.environ['BOOTSTRAPSERVERS']

        ignite_host = os.environ['IGNITE_SERVICE_NAME']
        ignite_port = int(os.environ['IGNITE_PORT'])
        
        cassandra_host = os.environ['CASS_HOST']
        cassandra_user_name = os.environ['CASS_USER_NAME']
        cassandra_user_password = os.environ['CASS_USER_PASSWORD']
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
     
    # Prepare Speaker Recognition Modules
    embedding_model = EncoderClassifier.from_hparams(
        source = SR_MODEL_SOURCE,
        savedir = SR_SAVEDIR
    )
    embedding_model.eval()
    embedding_model.zero_grad(set_to_none = True)
    sr_service = SrService(embedding_model = embedding_model)

    # Prepare Kafka Producer, Consumer
    kafka_config_dict = get_kafka_config(
            bootstrap_servers = bootstrap_servers, 
            group_id = KAFKA_GROUP_ID, 
            kafka_user_name = kafka_user_name, 
            kafka_user_password = kafka_user_password
    )
    producer = KafkaProducer(kafka_config_dict['producer'], logger = get_stream_logger('kafka-producer'))
    consumer = KafkaConsumer(kafka_config_dict['consumer'], logger = get_stream_logger('kafka-consumer'))
    
    # Prepare IgniteRepository
    ignite_repository = IgniteRepository(
            host = ignite_host,
            port = ignite_port
    )
    
    # Prepare CassandraRepository
    cassandra_repository = CassandraRepository(
            host = cassandra_host,
            username = cassandra_user_name,
            password = cassandra_user_password
    )

    # Load Main Object 
    main_obj = SrMain(
        producer = producer,
        consumer = consumer,
        ignite_repository = ignite_repository,
        sr_service = sr_service,
        cassandra_repository = cassandra_repository
    )
    
    # Start...
    main_obj.run_loop()

if __name__ == "__main__":
    main()
