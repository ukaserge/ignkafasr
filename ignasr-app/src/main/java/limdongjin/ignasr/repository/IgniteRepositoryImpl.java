package limdongjin.ignasr.repository;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.configuration.ClientConfiguration;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

// TODO refactoring ignite repository OR re-implement
//@Repository
public class IgniteRepositoryImpl implements IgniteRepository {
    private final ClientConfiguration clientConfiguration;
    public IgniteRepositoryImpl(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public <K, V> Mono<Mono<K>> putAsync(String cacheName, K key, V value){
        try(var client = Ignition.startClient(clientConfiguration)){
            Mono<Mono<K>> completionStageMono = Mono.just(1)
                    .flatMap(a -> {
                        CompletionStage<Mono<K>> monoCompletionStage = client.getOrCreateCacheAsync(cacheName)
                                .thenApply(objectObjectClientCache -> {
                                    return Mono.fromCompletionStage(objectObjectClientCache.putAsync(key, value).thenApply(v -> key));
                                });
                        Mono<Mono<K>> monoMonoK = Mono.fromCompletionStage(monoCompletionStage);
                        return monoMonoK;
                    });
            return completionStageMono;
        }
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
