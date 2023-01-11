package limdongjin.ignasr.repository;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

public class IgniteRepositoryImpl implements IgniteRepository {
    private final ClientConfiguration clientConfiguration;
    public IgniteRepositoryImpl(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public <K, V> Mono<Mono<K>> putAsync(String cacheName, K key, V value){
        Mono<IgniteClient> monoClient = Mono.just(clientConfiguration).map(Ignition::startClient);
        Mono<Mono<K>> monoMonoK = monoClient.flatMap(client -> {
            CompletionStage<Mono<K>> monoCompletionStage = client
                    .getOrCreateCacheAsync(cacheName)
                     // :: IgniteClientFuture<ClientCache>.thenApplyAsync(cache -> monoK)
                    .thenApplyAsync(cache ->
                            Mono.fromCompletionStage(cache.putAsync(key, value).thenApply(vo -> key))
                                .doOnSuccess(unused -> client.close())
                    )
            ;

            // monoMonoK
            return Mono.fromCompletionStage(monoCompletionStage);
        });

        return monoMonoK;
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
//    public <K, V> Mono<Mono<K>> putAsync2(String cacheName, K key, V value){
//            var client = Ignition.startClient(clientConfiguration);
//
//            CompletionStage<Mono<K>> monoCompletionStage = client.getOrCreateCacheAsync(cacheName)
//                    .thenApplyAsync(cache -> {
//                        return Mono.fromCompletionStage(cache.putAsync(key, value).thenApply(vo -> key)).doOnSuccess(unused -> client.close());
//                    });
//            Mono<Mono<K>> monoMonoK = Mono.fromCompletionStage(monoCompletionStage);
//            return monoMonoK;
//    }
}
