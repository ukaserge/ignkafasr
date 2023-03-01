package limdongjin.ignasr.repository

import reactor.core.publisher.Mono

interface IgniteRepository {
    fun <K: Any, V: Any> putAsync(cacheName: String, key: K, value: V): Mono<Mono<K>?>
    fun <K: Any, V: Any> put(cacheName: String, key: K, value: V): K
    fun <K: Any, V: Any> get(cacheName: String, key: K): V
    fun <K: Any, V: Any> size(cacheName: String): Int
}