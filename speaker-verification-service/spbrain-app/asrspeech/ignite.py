#!/usr/bin/env python3
from typing import Optional, Tuple, Any, Dict, List, Iterable, TypeVar, Callable
from pyignite.client import Cache, Client
K = TypeVar('K')
V = TypeVar('V')

class IgniteRepository:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.ignite_client = Client()

    def connect(self, on_connect: Callable[[Client], V]) -> V:
        with self.ignite_client.connect(self.host, self.port):
          return on_connect(self.ignite_client)
    
    @staticmethod
    def get_cache(connected_client: Client, cache_name: str, action: Callable[[Cache], V]) -> V:
        cache = connected_client.get_or_create_cache(cache_name)
        return action(cache)
      
    def get(self, cache_name: str, key: K) -> V:
        return self.connect(
            lambda client: IgniteRepository.get_cache(client, cache_name, 
                lambda cache: cache.get(key)
            )
        )
    
    def put(self, cache_name: str, key: K, value: V) -> None:
        self.connect(
            lambda client: IgniteRepository.get_cache(client, cache_name, 
                lambda cache: cache.put(key, value)
            )
        )
      
      
    def put_all(self, cache_name: str, key_value_dict: Dict[K, V]) -> None:
        self.connect(
            lambda client: IgniteRepository.get_cache(client, cache_name, 
                lambda cache: cache.put_all(key_value_dict)
            )
        )
      
    def get_all(self, cache_name: str, keys: List[K]) -> Dict[K, V]:
        return self.connect(
            lambda client: IgniteRepository.get_cache(client, cache_name,
                lambda cache: cache.get_all(keys)
            )
        )
    
    def scan(self, cache_name: str, only_key=False) -> Dict[K, V]:
        def action(cache: Cache) -> Dict[K, V]:
            key_value_dict = {}
            with cache.scan() as cursor:
                if only_key:
                    for key, _ in cursor:
                        key_value_dict[key] = 0
                else:
                    key_value_dict = dict(cursor)
            return key_value_dict
        
        return self.connect(
            lambda client: IgniteRepository.get_cache(client, cache_name, action)
        )