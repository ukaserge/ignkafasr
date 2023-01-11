package limdongjin.stomasr;

import limdongjin.stomasr.kafka.KafkaConstants;
import limdongjin.stomasr.kafka.SuccListener;
import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.service.SuccService;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.*;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

@ExtendWith(value = { SpringExtension.class, MockitoExtension.class })
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SuccIntegrationTest {
    final static String OK_MSG = "OK; ";
    final static String FAIL_MSG = "FAIL; ";
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("limdongjin.stomasr.kafka.bootstrapservers", kafkaContainer::getBootstrapServers);
    }
    @Autowired
    SuccListener succListener;

    @Autowired
    private AuthRepository authRepository;

    @Mock
    private SimpMessageSendingOperations sendingOperations;
    private SuccService succService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @BeforeEach
    void setUp() {
        this.succService = Mockito.spy(new SuccService(sendingOperations, authRepository));
        this.succListener.setSuccService(succService);
    }

    @Test
    void receiveMessageFromInferTopicThen() throws InterruptedException {
        Mockito.doNothing().when(sendingOperations).convertAndSend(Mockito.anyString(), Mockito.anyString());

        var uuid = UUID.randomUUID().toString();
        var payload = String.join(",", uuid, OK_MSG);


        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER, payload);
        Thread.sleep(10000);

        Mockito.verify(succService, Mockito.atLeast(1)).onInfer(payload);
        Mockito.verify(sendingOperations, Mockito.atLeast(1)).convertAndSend(MessageDestinationPrefixConstants.SUCC + uuid, OK_MSG);
        Assertions.assertTrue(authRepository.containsKey(uuid));
        Assertions.assertEquals(1, authRepository.size());
    }

    @Test
    void handleInvalidKafkaMessage() throws InterruptedException {
        var invalidUuid = "9-1234-5678";
        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER, invalidUuid + "," + OK_MSG);
        Thread.sleep(10000);

        Mockito.verify(sendingOperations, Mockito.never()).convertAndSend(Mockito.anyString());
        Assertions.assertEquals(0, authRepository.size());
    }
}

//@TestPropertySource (properties = {
//        "ALLOWORIGIN=http://localhost:9092",
//        "BOOTSTRAPSERVERS=http://localhost:9092",
//        "SASLJAASCONFIG=foo",
//        "MYPORT=8088"
//})