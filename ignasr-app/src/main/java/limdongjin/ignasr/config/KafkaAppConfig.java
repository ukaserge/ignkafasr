package limdongjin.ignasr.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.scram.internals.ScramMechanism;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaAppConfig {
     @Value("${limdongjin.ignasr.kafka.bootstrapservers}")
     public String bootstrapServers;

     @Value("${limdongjin.ignasr.kafka.sasljaasconfig}")
     public String saslJaasConfig;

     @Value("${limdongjin.ignasr.kafka.security-protocol}")
     public String securityProtocol;

     @Value("${limdongjin.ignasr.kafka.sasl-mechanism}")
     public String saslMechanism;

    @Bean
    public ReactiveKafkaProducerTemplate<String, byte[]> reactiveKafkaProducerTemplate() {
        return new ReactiveKafkaProducerTemplate<>(buildSenderOptions());
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

    @Bean
    public NewTopic userPendingTopic() {
        return TopicBuilder.name("user-pending")
                .partitions(3)
                .compact()
                .build();
    }
    

    public SenderOptions<String, byte[]> buildSenderOptions(){
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        if(securityProtocol.equals("SASL_PLAINTEXT")) {
            producerProps.put("security.protocol", "SASL_PLAINTEXT");
            producerProps.put("sasl.mechanism", "SCRAM-SHA-512");
            producerProps.put("sasl.jaas.config", saslJaasConfig);
        }

        return SenderOptions.<String, byte[]>create(producerProps)
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(new ByteArraySerializer())
        ;
    }
}
