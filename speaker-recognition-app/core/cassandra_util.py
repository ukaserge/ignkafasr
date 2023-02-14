from cassandra.cluster import Cluster, NoHostAvailable
from cassandra.auth import PlainTextAuthProvider

class CassandraRepository:
    def __init__(self, host, username, password):
        self.auth_provider = PlainTextAuthProvider(username=username, password=password)
        self.host = host
        self.session = None
        self.cluster = None
    
    def connect(self):
        while True:
            try:
                self.cluster = Cluster([self.host], auth_provider=self.auth_provider)
                self.connect_timeout = 60
                self.session = self.cluster.connect()
                self.session.default_timeout = 60
                break
            except NoHostAvailable as e:
                print(e)
    
    def close(self):
        self.cluster.shutdown()
        self.session = None
        self.cluster = None


    def execute(self, query, *args):
        assert self.session != None and self.cluster != None
        return self.session.execute(query, *args)


