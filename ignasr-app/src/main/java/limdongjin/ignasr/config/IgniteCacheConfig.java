package limdongjin.ignasr.config;

import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.ThinClientKubernetesAddressFinder;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.ThinClientConfiguration;
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.UUID;

@Configuration
public class IgniteCacheConfig {
    public static final String cacheName = "uploadCache";
    public static final CacheMode cacheMode = CacheMode.PARTITIONED;
    public static final CacheWriteSynchronizationMode cacheWriteSyncMode = CacheWriteSynchronizationMode.PRIMARY_SYNC;
    public static final CacheAtomicityMode cacheAtomicityMode = CacheAtomicityMode.ATOMIC;

    @Value("${limdongjin.ignasr.ignite.namespace}")
    public String namespace;

    @Value("${limdongjin.ignasr.ignite.servicename}")
    public String serviceName;

    @Value("${limdongjin.ignasr.ignite.addresses}")
    public String addresses;

    @Bean
    public ClientConfiguration clientConfiguration() {
        KubernetesConnectionConfiguration kcfg = new KubernetesConnectionConfiguration();
        kcfg.setNamespace(namespace); // limdongjin
        kcfg.setServiceName(serviceName); // ignite-service

        ClientConfiguration clientCfg = new ClientConfiguration();
        clientCfg.setAddresses(addresses);
        clientCfg.setAddressesFinder(new ThinClientKubernetesAddressFinder(kcfg));
        clientCfg.setTcpNoDelay(false);
        clientCfg.setPartitionAwarenessEnabled(true);
        clientCfg.setSendBufferSize(15*1024*1024);
        clientCfg.setReceiveBufferSize(15*1024*1024);

        IgniteClient cl = Ignition.startClient(clientCfg);
        ClientCacheConfiguration cacheCfg = buildDefaultClientCacheConfiguration(cacheName);
//        cl.destroyCache(cacheName);
        cl.<UUID, byte[]>getOrCreateCache(cacheCfg);
        cl.<UUID, UUID>getOrCreateCache(buildDefaultClientCacheConfiguration("authCache"));
        cl.close();

        return clientCfg;
    }

    private ClientCacheConfiguration buildDefaultClientCacheConfiguration(String name) {
        ClientCacheConfiguration cacheCfg = new ClientCacheConfiguration();
        cacheCfg.setName(name);
        cacheCfg.setCacheMode(cacheMode);
        cacheCfg.setWriteSynchronizationMode(cacheWriteSyncMode);
        cacheCfg.setAtomicityMode(cacheAtomicityMode);
        cacheCfg.setExpiryPolicy(new ExpiryPolicy() {
            @Override
            public Duration getExpiryForCreation() {
                return Duration.FIVE_MINUTES;
            }

            @Override
            public Duration getExpiryForAccess() {
                return Duration.FIVE_MINUTES;
            }

            @Override
            public Duration getExpiryForUpdate() {
                return Duration.FIVE_MINUTES;
            }
        });

//        cacheCfg.setPartitionLossPolicy(PartitionLossPolicy.READ_WRITE_SAFE);
//        cacheCfg.setBackups(3);
        // cacheCfg.setGroupName(cacheGroupName);

        return cacheCfg;
    }
}
