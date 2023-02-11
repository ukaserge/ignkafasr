from cassandra.cluster import Cluster
from cassandra.auth import PlainTextAuthProvider

class CassandraRepository:
    def __init__(self, host, username, password):
        self.auth_provider = PlainTextAuthProvider(username=username, password=password)
        self.cluster = Cluster([host], auth_provider=self.auth_provider)
        self.session = self.cluster.connect()

    def execute(self, query):
        return self.session.execute(query)

