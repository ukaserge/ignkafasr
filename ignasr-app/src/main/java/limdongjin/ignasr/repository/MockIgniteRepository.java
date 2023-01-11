package limdongjin.ignasr.repository;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public class MockIgniteRepository implements IgniteRepository{
    public ConcurrentHashMap<String, ConcurrentHashMap<Object, Object>> name2cache;

    public MockIgniteRepository() {
        this.name2cache = new ConcurrentHashMap<>();
    }

    @Override
    public <K, V> Mono<Mono<K>> putAsync(String cacheName, K key, V value) {
        Mono<K> kMono = Mono.just(key)
            .doOnNext(k -> {
                ConcurrentHashMap<K, V> cache;
                if(name2cache.containsKey(cacheName)){
                    cache = (ConcurrentHashMap<K, V>) name2cache.get(cacheName);
                }else {
                    cache = new ConcurrentHashMap<K, V>();
                    name2cache.put(cacheName, (ConcurrentHashMap<Object, Object>) cache);
                }
                cache.put(key, value);
            })
        ;
        Mono<Mono<K>> monoMonoK = Mono.just(kMono);
        return monoMonoK;
    }

    @Override
    public <K, V> K put(String cacheName, K key, V value) {
        ConcurrentHashMap<K, V> cache;
        if(name2cache.containsKey(cacheName)){
            cache = (ConcurrentHashMap<K, V>) name2cache.get(cacheName);
        }else {
            cache = new ConcurrentHashMap<K, V>();
            name2cache.put(cacheName, (ConcurrentHashMap<Object, Object>) cache);
        }
        cache.put(key, value);
        return key;
    }

    @Override
    public <K, V> V get(String cacheName, K key) {
        System.out.println(name2cache);
        ConcurrentHashMap<K, V> cache;
        if(name2cache.containsKey(cacheName)){
            cache = (ConcurrentHashMap<K, V>) name2cache.get(cacheName);
        }else {
            cache = new ConcurrentHashMap<K, V>();
            name2cache.put(cacheName, (ConcurrentHashMap<Object, Object>) cache);
        }
        return cache.get(key);
    }

    @Override
    public <K, V> int size(String cacheName) {
        int size;
        ConcurrentHashMap<K, V> cache;
        if(name2cache.containsKey(cacheName)){
            cache = (ConcurrentHashMap<K, V>) name2cache.get(cacheName);
        }else {
            cache = new ConcurrentHashMap<K, V>();
            name2cache.put(cacheName, (ConcurrentHashMap<Object, Object>) cache);
        }
        size = cache.size();
        return size;
    }
}
