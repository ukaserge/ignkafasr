package limdongjin.stomasr;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.dto.UserMessage;
import limdongjin.stomasr.kafka.KafkaConstants;
import limdongjin.stomasr.kafka.VerifSuccListener;
import limdongjin.stomasr.protos.InferProto;
import limdongjin.stomasr.service.JoinService;
import limdongjin.stomasr.service.SuccService;
import limdongjin.stomasr.stomp.JoinSubReceiver;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
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
    @Autowired
    @SpyBean
    private SimpMessageSendingOperations sendingOperations;

    @Autowired
    @SpyBean
    private SuccService succService;

    @Autowired
    private JoinService joinService;


    @Autowired
    @SpyBean
    private VerifSuccListener verifSuccListener;

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
    }

    @Test
    void receiveMessageFromInferTopicThen() throws InterruptedException, InvalidProtocolBufferException {
        Mockito.doNothing().when(sendingOperations).convertAndSendToUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        var uuid = UUID.randomUUID().toString();
        var uuid2 = UUID.randomUUID().toString();
        byte[] payload = InferProto.Infer.newBuilder().setUserId(uuid2).setReqId(uuid).setInferResult(OK_MSG).build().toByteArray();

        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER_VERIFICATION, payload);
        Thread.sleep(10000);

        Mockito.verify(succService, Mockito.atLeast(1)).onInferVerif(Mockito.any());
        Mockito.verify(sendingOperations, Mockito.atLeast(1)).convertAndSendToUser(Mockito.anyString() ,Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void handleInvalidKafkaMessage() throws InterruptedException {
        var invalidUuid = "9-1234-5678";
        Thread.sleep(10000);
        kafkaTemplate.send(KafkaConstants.TOPIC_INFER_VERIFICATION, InferProto.Infer.newBuilder()
                .setUserId(invalidUuid)
                .setReqId(invalidUuid)
                .setInferResult("OK")
                .build()
                .toByteArray())
        ;
        Thread.sleep(10000);

        Mockito.verify(sendingOperations, Mockito.never()).convertAndSend(Mockito.anyString());
    }

    @Test
    void clientSideStompTest() throws ExecutionException, InterruptedException, TimeoutException {
        var joinServiceSpy = Mockito.spy(joinService);
        joinSubReceiver.setJoinService(joinServiceSpy);

        String url = "ws://localhost:8089/ws-stomp";

        List<Transport> webSocketTransports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient webSocketStompClient = new WebSocketStompClient(new SockJsClient(webSocketTransports));
        webSocketStompClient.setMessageConverter(new MappingJackson2MessageConverter());

        var userId = UUID.randomUUID().toString();
        var reqId = UUID.randomUUID().toString();

        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.set("user", userId);

        StompSession stompSession = webSocketStompClient
                .connect(url,new WebSocketHttpHeaders(), stompHeaders , new MyStompSessionHandlerAdapter())
                .get(5, TimeUnit.SECONDS);

        var destination = "/user/topic/succ";

        var header = new StompHeaders();
        header.setDestination(destination);
        StompSession.Subscription subscribe = stompSession.subscribe(header, new MyStompFrameHandler());
        Thread.sleep(5000);

        Thread.sleep(5000);

        kafkaTemplate.send(KafkaConstants.TOPIC_INFER_VERIFICATION, InferProto.Infer.newBuilder().setUserId(userId).setReqId(reqId).setInferResult(OK_MSG).build().toByteArray());
        Thread.sleep(100000);
        // if client send msg to '/app/join', then joinSubReceiver receive msg and invoke JoinService
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
