from testcontainers.kafka import KafkaContainer
from testcontainers.core.generic import GenericContainer
from testcontainers.core.container import DockerContainer

def canLoad():
    ignite_container: DockerContainer = DockerContainer("apacheignite/ignite:2.14.0")\
        .with_bind_ports(10800, 20800)\
        .with_exposed_ports(20800)
    kafka_container: KafkaContainer = KafkaContainer()

    kafka_container.start()
    ignite_container.start()

    print(ignite_container.get_exposed_port(10800))
    print(kafka_container.get_bootstrap_server())

