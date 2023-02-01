package limdongjin.ignasr.config

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.ScramMechanism
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import reactor.kafka.sender.SenderOptions


@Configuration
open class KafkaAppConfig(
    @Value("\${limdongjin.ignasr.kafka.bootstrapservers}")
    private val bootstrapServers: String,

    @Value("\${limdongjin.ignasr.kafka.sasljaasconfig}")
    private val saslJaasConfig: String,

    @Value("\${limdongjin.ignasr.kafka.security-protocol}")
    private val securityProtocol: String,

    @Value("\${limdongjin.ignasr.kafka.sasl-mechanism}")
    private val saslMechanism: String,
){
    @Bean
    fun reactiveKafkaProducerTemplate(): ReactiveKafkaProducerTemplate<String, ByteArray> {
        return ReactiveKafkaProducerTemplate(buildSenderOptions())
    }

    @Bean
    fun kafkaAdmin(): KafkaAdmin? {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        if (securityProtocol == SecurityProtocol.SASL_PLAINTEXT.name) {
            configs[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] =
                SecurityProtocol.SASL_PLAINTEXT.name // SASL_PLAINTEXT
            configs[SaslConfigs.SASL_MECHANISM] = ScramMechanism.SCRAM_SHA_512.mechanismName() // SCRAM-SHA-512
            configs[SaslConfigs.SASL_JAAS_CONFIG] = saslJaasConfig
        }
        return KafkaAdmin(configs)
    }

    fun buildSenderOptions(): SenderOptions<String, ByteArray> {
        val producerProps = mutableMapOf<String, Any>();
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArray::class.java
        producerProps[ProducerConfig.RETRIES_CONFIG] = 30
        producerProps[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 30000

        if(securityProtocol.equals("SASL_PLAINTEXT")){
            producerProps["security.protocol"] = "SASL_PLAINTEXT"
            producerProps["sasl.mechanism"] = "SCRAM-SHA-512"
            producerProps["sasl.jaas.config"] = saslJaasConfig
        }
        return SenderOptions.create<String?, ByteArray?>(producerProps)
            .withKeySerializer(StringSerializer())
            .withValueSerializer(ByteArraySerializer())
            .maxInFlight(1024)
    }
}