from testcontainers.kafka import KafkaContainer
from testcontainers.core.container import DockerContainer
from confluent_kafka import Consumer, Producer, OFFSET_BEGINNING
from asrspeech.protobuf import userpending_pb2
import uuid

def test_integration():
    # ignite_container: DockerContainer = DockerContainer("apacheignite/ignite:2.14.0") \
        # .with_bind_ports(10800, 20800) \
        # .with_exposed_ports(20800)

    kafka_container: KafkaContainer = KafkaContainer(port_to_expose=29093)
    # kafka_container: DockerContainer = DockerContainer("confluentinc/cp-zookeeper:latest") \
        # .with_bind_ports(909)
    
    kafka_container.start()
    # ignite_container.start()

    # print(ignite_container.get_exposed_port(10800))
    bootstrap_servers = kafka_container.get_bootstrap_server()
    print(bootstrap_servers)

    producer: Producer = Producer({
        'bootstrap.servers': bootstrap_servers,
        'queue.buffering.max.ms': 500,
        'batch.num.messages': 50,
    })
    consumer: Consumer = Consumer({
        'bootstrap.servers': bootstrap_servers,
        # 'max.poll.interval.ms': 60000,
        'enable.auto.commit': True,
        'group.id': 'my-group',
        'auto.offset.reset': 'earliest'
    })
    
    def reset_offset_beginning(topic_consumer, partitions):
        for p in partitions:
            p.offset = OFFSET_BEGINNING
        topic_consumer.assign(partitions)


    consumer.subscribe(['user-pending'], on_assign=reset_offset_beginning)
    userpending = userpending_pb2.UserPending()
    userpending.reqId = str(uuid.uuid4())
    producer.produce(topic='user-pending', value=userpending.SerializeToString())
    producer.poll(0)
    producer.flush()
    import time
    time.sleep(30)
    msg = consumer.poll(1.0)
    print(msg.value())
    
    u = userpending_pb2.UserPending.FromString(msg.value())
    
if __name__ == "__main__":
    test_integration()