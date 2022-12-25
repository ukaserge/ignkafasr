package limdongjin.ignasr.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;
import reactor.util.Loggers;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaAppConfig {
     @Value("${limdongjin.ignasr.kafka.bootstrapservers}")
     public String bootstrapServers;

     @Value("${limdongjin.ignasr.kafka.sasljaasconfig}")
     public String saslJaasConfig;

    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate() {
        return new ReactiveKafkaProducerTemplate<>(buildSenderOptions());
    }
    public SenderOptions<String, String> buildSenderOptions(){
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producerProps.put("security.protocol", "SASL_PLAINTEXT");
        producerProps.put("sasl.mechanism", "SCRAM-SHA-512");
        producerProps.put("sasl.jaas.config", saslJaasConfig);

        return SenderOptions.<String, String>create(producerProps)
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(new StringSerializer())
        ;
    }
}
