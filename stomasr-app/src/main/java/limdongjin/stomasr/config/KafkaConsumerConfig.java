package limdongjin.stomasr.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
     @Value("${limdongjin.stomasr.kafka.bootstrapservers}")
     public String bootstrapServers;

     @Value("${limdongjin.stomasr.kafka.sasljaasconfig}")
     public String saslJaasConfig;

    @Value("${limdongjin.stomasr.kafka.security-protocol}")
    public String securityProtocol;

    @Value("${limdongjin.stomasr.kafka.sasl-mechanism}")
    public String saslMechanism;

    @Bean
    public ConsumerFactory<Object, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        if(securityProtocol.equals(SecurityProtocol.SASL_PLAINTEXT.name)){
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
            props.put(SaslConfigs.SASL_MECHANISM, ScramMechanism.SCRAM_SHA_512.mechanismName());
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        if(securityProtocol.equals(SecurityProtocol.SASL_PLAINTEXT.name)){
            configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name); // SASL_PLAINTEXT
            configs.put(SaslConfigs.SASL_MECHANISM, ScramMechanism.SCRAM_SHA_512.mechanismName()); // SCRAM-SHA-512
            configs.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }

        return new KafkaAdmin(configs);
    }
}
