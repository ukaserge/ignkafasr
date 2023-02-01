package limdongjin.ignasr.repository

import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap


class MockIgniteRepository: IgniteRepository {
    val name2cache: ConcurrentHashMap<String, ConcurrentHashMap<Any, Any>> =
        ConcurrentHashMap();

    override fun <K : Any, V : Any> putAsync(cacheName: String, key: K, value: V): Mono<Mono<K>?> {
        val kMono: Mono<K> =
            Mono.just<K>(key)
                .doOnNext { k: K ->
                    val cache = name2cache.getOrPut(cacheName) { ConcurrentHashMap() }
                    cache!![key] = value
                }
        return Mono.just<Mono<K>>(kMono)
    }

    override fun <K: Any, V: Any> put(cacheName: String, key: K, value: V): K {
        val cache = name2cache.getOrPut(cacheName) { ConcurrentHashMap() }
        cache[key] = value
        return key
    }

    override fun <K: Any, V: Any> get(cacheName: String, key: K): V {
        val cache = name2cache.getOrPut(cacheName) { ConcurrentHashMap() }
        return cache[key]!! as V
    }

    override fun <K: Any, V: Any> size(cacheName: String): Int {
        val cache = name2cache.getOrPut(cacheName) { ConcurrentHashMap() }
        return cache.size
    }
}