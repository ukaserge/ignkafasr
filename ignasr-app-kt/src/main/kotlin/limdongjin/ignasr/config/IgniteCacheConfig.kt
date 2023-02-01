package limdongjin.ignasr.config

import limdongjin.ignasr.repository.IgniteRepository
import limdongjin.ignasr.repository.IgniteRepositoryImpl
import limdongjin.ignasr.repository.MockIgniteRepository
import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheWriteSynchronizationMode
import org.apache.ignite.client.ClientCacheConfiguration
import org.apache.ignite.client.ThinClientKubernetesAddressFinder
import org.apache.ignite.configuration.ClientConfiguration
import org.apache.ignite.kubernetes.configuration.KubernetesConnectionConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import javax.cache.expiry.Duration
import javax.cache.expiry.ExpiryPolicy

@Configuration
class IgniteCacheConfig(
    @Value("\${limdongjin.ignasr.ignite.namespace}")
    val namespace: String,

    @Value("\${limdongjin.ignasr.ignite.servicename}")
    val serviceName: String,

    @Value("\${limdongjin.ignasr.ignite.addresses}")
    val addresses: String,

    @Value("\${limdongjin.ignasr.ignite.mode}")
    val igniteMode: String
){
    @Bean
    fun igniteRepository(clientConfiguration: ClientConfiguration): IgniteRepository {
        return if (igniteMode == "mock") {
            MockIgniteRepository()
        } else IgniteRepositoryImpl(clientConfiguration)
    }

    // TODO refactoring
    @Bean
    @Throws(InterruptedException::class)
    fun clientConfiguration(): ClientConfiguration {
        val clientCfg = ClientConfiguration()
        if (igniteMode == "mock") {
            return clientCfg
        }
        clientCfg.let {
            if (igniteMode == "kubernetes") {
                val kcfg = KubernetesConnectionConfiguration()
                kcfg.namespace = namespace // limdongjin
                kcfg.serviceName = serviceName // ignite-service
                it.addressesFinder = ThinClientKubernetesAddressFinder(kcfg)
            }
            it.setAddresses(addresses)
            it.isTcpNoDelay = DEFAULT_IS_TCP_DELAY
            it.isPartitionAwarenessEnabled = DEFAULT_IS_PARTITION_AWARENESS_ENABLED
            it.sendBufferSize = DEFAULT_SEND_BUFFER_SIZE
            it.receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE

            var i = 0
            var flag = true
            while (flag) {
                i += 1
                if(i == 10) {
                    throw RuntimeException("can't connect ignite")
                }
                try {
                    Ignition.startClient(it).use { cl ->
                        //        ClientCacheConfiguration cacheCfg = buildDefaultClientCacheConfiguration(cacheName);
                        //        cl.destroyCache(cacheName);
                        cl.getOrCreateCache<UUID, ByteArray>(
                            buildDefaultClientCacheConfiguration(
                                "uploadCache",
                                DEFAULT_EXPIRY_DURATION
                            )
                        )
                        cl.getOrCreateCache<UUID, UUID>(
                            buildDefaultClientCacheConfiguration(
                                "authCache",
                                DEFAULT_EXPIRY_DURATION
                            )
                        )
                        cl.getOrCreateCache<UUID, UUID>(
                            buildDefaultClientCacheConfiguration(
                                "reqId2userId",
                                DEFAULT_EXPIRY_DURATION
                            )
                        )
                        cl.getOrCreateCache<UUID, String>(
                            buildDefaultClientCacheConfiguration(
                                "uuid2label",
                                DEFAULT_EXPIRY_DURATION
                            )
                        )
                        flag = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Thread.sleep(2000)
        }
        return clientCfg
    }

    companion object {
        val DEFAULT_CACHE_MODE = CacheMode.PARTITIONED
        val DEFAULT_WRITE_SYNC_MODE = CacheWriteSynchronizationMode.PRIMARY_SYNC
        val DEFAULT_ATOMICITY_MODE = CacheAtomicityMode.ATOMIC
        val DEFAULT_QUERY_PARALLELISM = 5
        val DEFAULT_IS_TCP_DELAY = false
        val DEFAULT_IS_PARTITION_AWARENESS_ENABLED = true
        val DEFAULT_SEND_BUFFER_SIZE = 15 * 1024 * 1024
        val DEFAULT_RECEIVE_BUFFER_SIZE = 15 * 1024 * 1024
        val DEFAULT_EXPIRY_DURATION = Duration.ONE_HOUR

        private fun buildDefaultClientCacheConfiguration(
            cacheName: String,
            expiryDuration: Duration,
        ): ClientCacheConfiguration {
            val cacheCfg = ClientCacheConfiguration()
            with(cacheCfg){
                name = cacheName
                cacheMode = DEFAULT_CACHE_MODE
                writeSynchronizationMode = DEFAULT_WRITE_SYNC_MODE
                atomicityMode = DEFAULT_ATOMICITY_MODE
                queryParallelism = DEFAULT_QUERY_PARALLELISM

                expiryPolicy = object : ExpiryPolicy {
                    override fun getExpiryForCreation(): Duration {
                        return expiryDuration
                    }

                    override fun getExpiryForAccess(): Duration {
                        return expiryDuration
                    }

                    override fun getExpiryForUpdate(): Duration {
                        return expiryDuration
                    }
                }
            }
            return cacheCfg
        }
    }
}