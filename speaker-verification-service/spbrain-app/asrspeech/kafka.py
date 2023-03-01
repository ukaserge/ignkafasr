from typing import Dict, Optional

# TODO move to configMap or environmentVariable,..
def get_kafka_config(bootstrap_servers: str, 
                     group_id: str,
                     kafka_user_name: Optional[str] = None,
                     kafka_user_password: Optional[str] = None) -> Dict[str, Dict[str, str]]:
  kafka_config: Dict[str, Dict[str, str]] = {}
  kafka_config['producer'] = {
      'bootstrap.servers': bootstrap_servers
#      'queue.buffering.max.ms': 500,
#      'batch.num.messages': 50,
#      'acks': 0,
#      'debug': 'broker,topic,msg',
#      'max.poll.interval.ms': 420000,
#      'queue.buffering.max.ms': 36000,
#      'linger.ms': 36000
  }
  kafka_config['consumer'] = {
      'bootstrap.servers': bootstrap_servers,
#      'max.poll.interval.ms': 420000,
#      'heartbeat.interval.ms': 10000,
#      'session.timeout.ms': 30000,
      'group.id': group_id,
      'auto.offset.reset': 'earliest',
      'enable.auto.commit': False,
      'enable.auto.offset.store': False,
      # "partition.assignment.strategy": partition_assignment_strategy,
      # "debug": "consumer,cgrp,topic,fetch",
      "on_commit": on_commit_completed_print
  }
  if kafka_user_name is not None:
      kafka_auth_config = {
          'sasl.mechanism': 'SCRAM-SHA-512',
          'security.protocol': 'SASL_PLAINTEXT',
          'sasl.username': kafka_user_name,
          'sasl.password': kafka_user_password,
      }
      kafka_config['producer'] = dict(kafka_config['producer'], **kafka_auth_config)
      kafka_config['consumer'] = dict(kafka_config['consumer'], **kafka_auth_config)
      
  return kafka_config

def on_acked_print(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Message produced: %s" % (str(msg)))

def on_commit_completed_print(err, partitions):
    if err:
        print(str(err))
    else:
        print("Committed partition offsets: " + str(partitions))

def print_assignment(topic_consumer, partitions):
    print('Assignment:', partitions)
    
