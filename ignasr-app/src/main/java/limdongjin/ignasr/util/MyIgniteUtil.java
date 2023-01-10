package limdongjin.ignasr.util;

import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;

import java.util.function.BiFunction;

public class MyIgniteUtil {
    public static <K, V> BiFunction<K, V, K> functionForPutIntoCache(IgniteClient client, String cacheName) {
        return (K key, V value) -> {
            ClientCache<K, V> cache = client.getOrCreateCache(cacheName);
            cache.put(key, value);
            return key;
        };
    }
}
