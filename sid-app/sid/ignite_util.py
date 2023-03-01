#!/usr/bin/env python3
from typing import Optional, Tuple, Any, Dict, List, Iterable, TypeVar, Callable
from pyignite.client import Cache, Client
from pyignite.datatypes import ByteArray
from pyignite.exceptions import SocketError

K = TypeVar('K')
V = TypeVar('V')

class IgniteTemplate:
    def __init__(self, hosts, port):
        assert hosts is not None
        
        self.ignite_info = [(host, port) for host in hosts.split(',')]
    
    def load_ignite_client(self):
        return Client(partition_aware=True)

    def connect_and(self, client, on_connect: Callable[[Client], V]) -> V:
        with client.connect(self.ignite_info):
            try:
                return on_connect(client)
            except (OSError, SocketError) as e:
                raise e

    def get_cache(self, connected_client: Client, cache_name: str, action: Callable[[Cache], V]) -> V:
        cache = connected_client.get_cache(cache_name)
        return action(cache)
      
    def get(self, cache_name: str, key: K, client = None) -> V:
        after_close = False
        if client is None:
            client = self.load_ignite_client()
            after_close = True

        result = self.connect_and(
            client, 
            lambda cl: self.get_cache(
                cl, 
                cache_name, 
                lambda cache: cache.get(key)
            )
        )

        if after_close:
            client.close()
        return result
    
    def put(self, cache_name: str, key: K, value: V, client = None) -> None:
        after_close = False
        if client is None:
            client = self.load_ignite_client()
            after_close = True

        self.connect_and(
            client,
            lambda cl: self.get_cache(
                cl, 
                cache_name, 
                lambda cache: cache.put(key, value)
            )     
        )

        if after_close:
            client.close()
      
    def put_all(self, cache_name: str, key_value_dict: Dict[K, V], client = None) -> None:
        after_close = False
        if client is None:
            client = self.load_ignite_client()
            after_close = True

        self.connect_and(
            client,
            lambda cl: self.get_cache(
                cl, 
                cache_name, 
                lambda cache: cache.put_all(key_value_dict)
            )
        )

        if after_close:
            client.close()
      
    def get_all(self, cache_name: str, keys: List[K], client = None) -> Dict[K, V]:
        after_close = False
        if client is None:
            client = self.load_ignite_client()
            after_close = True

        result = self.connect_and(
            client,
            lambda cl: self.get_cache(
                cl, 
                cache_name,
                lambda cache: cache.get_all(keys)
            )
        )

        if after_close:
            client.close()

        return result
    
    def scan(self, cache_name: str, only_key=False, client=None) -> Dict[K, V]:
        def action(cache: Cache) -> Dict[K, V]:
            key_value_dict = {}
            with cache.scan() as cursor:
                if only_key:
                    for key, _ in cursor:
                        key_value_dict[key] = 0
                else:
                    key_value_dict = dict(cursor)
            return key_value_dict

        after_close = False
        if client is None:
            client = self.load_ignite_client()
            after_close = True
                        
        result = self.connect_and(
            client,
            lambda cl: self.get_cache(cl, cache_name, action)
        )

        if after_close:
            client.close()
        return result
    
"""
def load_ignite_client():
    return Client()

def connect_and(client, host, port, on_connect: Callable[[Client], V]) -> V:
    with client.connect(host, port):
        try:
            return on_connect(client)
        except (OSError, SocketError) as e:
            raise e

def get_cache(connected_client: Client, cache_name: str, action: Callable[[Cache], V]) -> V:
    cache = connected_client.get_or_create_cache(cache_name)
    return action(cache)
      
def get(cache_name: str, key: K, client, host, port) -> V:
    assert False
    return connect_and(
        client, 
        host,
        port,
        lambda cl: get_cache(
            cl, 
            cache_name, 
            lambda cache: cache.get(key)
        )
    )
    
def put(cache_name: str, key: K, value: V, client, host, port) -> None:
    assert False
    connect_and(
        client,
        host,
        port,
        lambda cl: get_cache(
            cl, 
            cache_name, 
            lambda cache: cache.put(key, value)
        )     
     )
      
      
def put_all(cache_name: str, key_value_dict: Dict[K, V], client, host, port) -> None:
    connect_and(
        client,
        host,
        port,
        lambda cl: get_cache(
            cl, 
            cache_name, 
            lambda cache: cache.put_all(key_value_dict)
        )
    )
      
def get_all(cache_name: str, keys: List[K], client, host, port) -> Dict[K, V]:
    return connect_and(
        client,
        host,
        port,
        lambda cl: get_cache(
            cl, 
            cache_name,
            lambda cache: cache.get_all(keys)
        )
    )
    
def scan(cache_name: str, only_key=False, client=None, host=None, port=None) -> Dict[K, V]:
    def action(cache: Cache) -> Dict[K, V]:
        key_value_dict = {}
        with cache.scan() as cursor:
            if only_key:
                for key, _ in cursor:
                    key_value_dict[key] = 0
            else:
                key_value_dict = dict(cursor)
        return key_value_dict
        
    return connect_and(
        client,
        host,
        port,
        lambda cl: get_cache(cl, cache_name, action)
    )
"""
