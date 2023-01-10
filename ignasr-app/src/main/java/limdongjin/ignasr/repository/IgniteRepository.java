package limdongjin.ignasr.repository;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.stereotype.Repository;

// TODO refactoring ignite repository OR re-implement
@Repository
public class IgniteRepository {
    private final ClientConfiguration clientConfiguration;
    public IgniteRepository(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public <K, V> K put(String cacheName, K key, V value){
        try(var client = Ignition.startClient(clientConfiguration)){
            ClientCache<K, V> cache = client.getOrCreateCache(cacheName);
            cache.put(key, value);
            return key;
        }
    }

    public <K, V> V get(String cacheName, K key) {
        try(var client = Ignition.startClient(clientConfiguration)){
            ClientCache<K, V> cache = client.getOrCreateCache(cacheName);
            return cache.get(key);
        }
    }

    public <K, V> int size(String cacheName) {
        try(var client = Ignition.startClient(clientConfiguration)){
            ClientCache<K, V> cache = client.getOrCreateCache(cacheName);
            return cache.size();
        }
    }
}
