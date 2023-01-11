package limdongjin.ignasr.repository;

import reactor.core.publisher.Mono;

public interface IgniteRepository {
    public <K, V> Mono<Mono<K>> putAsync(String cacheName, K key, V value);
    public <K, V> K put(String cacheName, K key, V value);
    public <K, V> V get(String cacheName, K key);
    public <K, V> int size(String cacheName);
}
