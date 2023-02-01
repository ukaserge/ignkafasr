package limdongjin.ignasr.repository

import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration
import reactor.core.publisher.Mono
import java.util.concurrent.CompletionStage


class IgniteRepositoryImpl(private val clientConfiguration: ClientConfiguration) : IgniteRepository {
    override fun <K: Any, V: Any> putAsync(cacheName: String, key: K, value: V): Mono<Mono<K>?> {
        val monoClient: Mono<IgniteClient> = Mono.just(clientConfiguration).map(Ignition::startClient)
        return monoClient.flatMap { client ->
            val monoCompletionStage: CompletionStage<Mono<K>> = client
                .getOrCreateCacheAsync<K, V>(cacheName)
                .thenApplyAsync { cache ->
                    Mono.fromCompletionStage(cache.putAsync(key, value).thenApply { _ -> key })
                        .doOnSuccess { _ -> client.close() }
                }
            Mono.fromCompletionStage(monoCompletionStage)
        }
    }

    override fun <K: Any, V: Any> put(cacheName: String, key: K, value: V): K {
        Ignition.startClient(clientConfiguration).use { client ->
            val cache = client.getOrCreateCache<K, V>(cacheName)
            cache.put(key, value)
            return key
        }
    }

    override operator fun <K: Any, V: Any> get(cacheName: String, key: K): V {
        Ignition.startClient(clientConfiguration).use { client ->
            val cache = client.getOrCreateCache<K, V>(cacheName)
            return cache[key]
        }
    }

    override fun <K: Any, V: Any> size(cacheName: String): Int {
        Ignition.startClient(clientConfiguration).use { client ->
            val cache = client.getOrCreateCache<K, V>(cacheName)
            return cache.size()
        }
    }
}