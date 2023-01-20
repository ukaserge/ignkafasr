package limdongjin.stomasr;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.dto.UserMessage;
import limdongjin.stomasr.kafka.KafkaConstants;
import limdongjin.stomasr.kafka.SuccListener;
import limdongjin.stomasr.protos.InferProto;
import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.service.JoinService;
import limdongjin.stomasr.service.SuccService;
import limdongjin.stomasr.stomp.JoinSubReceiver;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;
//import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.std.StringSerializer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
//    @Autowired
    SuccListener succListener;

    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private SimpMessageSendingOperations sendingOperations;

    private SuccService succService;

    @Autowired
    private JoinService joinService;

    @Autowired
    private JoinSubReceiver joinSubReceiver;

    KafkaTemplate<String, byte[]> kafkaTemplate;

    @BeforeEach
    void setUp() {
        var senderOptions = new HashMap<String, Object>();
        senderOptions.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        senderOptions.put(ProducerConfig.SECURITY_PROVIDERS_CONFIG, SecurityProtocol.PLAINTEXT.name);
        senderOptions.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        senderOptions.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        this.kafkaTemplate = new KafkaTemplate<String, byte[]>(new DefaultKafkaProducerFactory<String, byte[]>(senderOptions));

        this.sendingOperations = Mockito.spy(sendingOperations);
        this.succService = Mockito.spy(new SuccService(sendingOperations, authRepository));
        this.succListener = new SuccListener(succService);
    }

    @Test
    void receiveMessageFromInferTopicThen() throws InterruptedException, InvalidProtocolBufferException {
        Mockito.doNothing().when(sendingOperations).convertAndSend(Mockito.anyString(), Mockito.anyString());

        var uuid = UUID.randomUUID().toString();
        byte[] payload = InferProto.Infer.newBuilder().setReqId(uuid).setInferResult(OK_MSG).build().toByteArray();

        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER, payload);
        Thread.sleep(10000);

        Mockito.verify(succService, Mockito.atLeast(1)).onInfer(payload);
        Mockito.verify(sendingOperations, Mockito.atLeast(1)).convertAndSend(Mockito.eq(MessageDestinationPrefixConstants.SUCC + uuid), Mockito.anyString());
        Assertions.assertTrue(authRepository.containsKey(uuid));
        Assertions.assertEquals(1, authRepository.size());
    }

    @Test
    void handleInvalidKafkaMessage() throws InterruptedException {
        var invalidUuid = "9-1234-5678";
        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER, InferProto.Infer.newBuilder().setReqId(invalidUuid).setInferResult("OK").build().toByteArray());
        Thread.sleep(10000);

        Mockito.verify(sendingOperations, Mockito.never()).convertAndSend(Mockito.anyString());
        Assertions.assertEquals(0, authRepository.size());
    }

    @Test
    void clientSideStompTest() throws ExecutionException, InterruptedException, TimeoutException {
        var joinServiceSpy = Mockito.spy(joinService);
        joinSubReceiver.setJoinService(joinServiceSpy);

        String url = "ws://localhost:8089/ws-stomp";

        List<Transport> webSocketTransports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient webSocketStompClient = new WebSocketStompClient(new SockJsClient(webSocketTransports));
        webSocketStompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession stompSession = webSocketStompClient.connect(url, new MyStompSessionHandlerAdapter()).get(5, TimeUnit.SECONDS);

        var reqId = UUID.randomUUID().toString();
        var destination = MessageDestinationPrefixConstants.SUCC + reqId;

        stompSession.send("/app/join", new UserMessage(reqId, "hello world!"));
        Thread.sleep(5000);

        // if client send msg to '/app/join', then joinSubReceiver receive msg and invoke JoinService
        Mockito.verify(joinServiceSpy, Mockito.atLeast(1)).join(Mockito.anyString());

        StompFrameHandler stompFrameHandler = new MyStompFrameHandler();
        StompSession.Subscription subscription = stompSession.subscribe(destination, stompFrameHandler);

        Thread.sleep(5000);

        System.out.println(subscription);

        kafkaTemplate.send(KafkaConstants.TOPIC_INFER, InferProto.Infer.newBuilder().setReqId(reqId).setInferResult(OK_MSG).build().toByteArray());
        Thread.sleep(5000);

        // if receive msg from 'infer', then handle and invoke convertAndSend
        Mockito.verify(sendingOperations, Mockito.atLeast(1)).convertAndSend(Mockito.eq(destination), Mockito.anyString());
    }
    private class MyStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return UserMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("[stompFrameHandler] handleFrame");
            System.out.println(payload);
        }
    }
    private class MyStompSessionHandlerAdapter extends StompSessionHandlerAdapter {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            System.out.println("getPayLoadType");
            System.out.println(headers.toString());

            return UserMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            System.out.println("handleFrame");
            if(payload != null){
                System.out.println("handle frame with payload: {}" + payload);
            }
            super.handleFrame(headers, payload);
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("afterConnected");
            System.out.println(session.toString());
            System.out.println(connectedHeaders.toString());

            super.afterConnected(session, connectedHeaders);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            System.out.println("handleException");
            System.out.println(session);
            System.out.println(command);
            System.out.println(new String(payload));
            exception.printStackTrace();

            super.handleException(session, command, headers, payload, exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.out.println("handleTransportError");
            System.out.println(session);
            exception.printStackTrace();

            super.handleTransportError(session, exception);
        }
    }

}

//@TestPropertySource (properties = {
//        "ALLOWORIGIN=http://localhost:9092",
//        "BOOTSTRAPSERVERS=http://localhost:9092",
//        "SASLJAASCONFIG=foo",
//        "MYPORT=8088"
//})