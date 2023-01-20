package limdongjin.ignasr.config;

import limdongjin.ignasr.protos.UserPendingProto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
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
