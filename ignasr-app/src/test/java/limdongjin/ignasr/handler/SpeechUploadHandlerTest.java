package limdongjin.ignasr.handler;

import limdongjin.ignasr.MyTestUtil;
import limdongjin.ignasr.repository.IgniteRepository;
import limdongjin.ignasr.repository.MockIgniteRepository;
import limdongjin.ignasr.router.SpeechRouter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SpeechUploadHandlerTest {
    IgniteRepository igniteRepository;
    @Mock
    ReactiveKafkaProducerTemplate<String, byte[]> reactiveKafkaProducerTemplate;

    SpeechUploadHandler speechUploadHandler;

    @BeforeEach
    void setUp() {
        this.igniteRepository = new MockIgniteRepository();
        this.speechUploadHandler = new SpeechUploadHandler(igniteRepository, reactiveKafkaProducerTemplate);
    }

    @Test
    void uploadHandlerFunctionTestByInvokeDirectly() throws InterruptedException, IOException {
        // given file for testing
        ClassPathResource wavClassPathResource = new ClassPathResource("data/foo.wav");
        String uuidString = UUID.randomUUID().toString();
        byte[] fileContent = wavClassPathResource.getInputStream().readAllBytes();
        String label = "limdongjin";

        // Stubbing
        String cacheName = "uploadCache";
        String cacheName2 = "uuid2label";

        Mockito.when(reactiveKafkaProducerTemplate.send(Mockito.anyString(), Mockito.any(byte[].class)))
                .thenReturn(Mono.just(MyTestUtil.emptySenderResultVoid()));

        // Prepare ServerRequest
        ServerRequest request = MyTestUtil.prepareServerRequest(wavClassPathResource, uuidString, label);
        System.out.println(request);

        // Directly Invoke Handler Function
        speechUploadHandler.upload(request).block();

        // Verify Behaviours
        Assertions.assertEquals(1, igniteRepository.size(cacheName));
        Assertions.assertEquals(
            new String(fileContent),
            new String(igniteRepository.<UUID, byte[]>get(cacheName, UUID.fromString(uuidString)))
        );
        Mockito.verify(reactiveKafkaProducerTemplate, Mockito.times(1)).send(Mockito.anyString(), Mockito.any(byte[].class));

    }

    @Test
    void registerHandlerFunctionTestByInvokeDirectly() throws IOException, InterruptedException {
        // given file for testing
        ClassPathResource wavClassPathResource = new ClassPathResource("data/foo.wav");
        String uuidString = UUID.randomUUID().toString();
        byte[] fileContent = wavClassPathResource.getInputStream().readAllBytes();
        String label = "limdongjin";

        // Prepare ServerRequest
        ServerRequest request = MyTestUtil.prepareServerRequest(wavClassPathResource, uuidString, label);
        System.out.println(request);

        // Directly Invoke Handler Function
        speechUploadHandler.register(request).block();

        // Verify Behaviours
        String uploadCache = "uploadCache";
        String cacheName2 = "uuid2label";
        String cacheName3 = "authCache";

        Assertions.assertEquals(1, igniteRepository.size(uploadCache));
        Assertions.assertEquals(
                new String(fileContent),
                new String(igniteRepository.<UUID, byte[]>get(uploadCache, UUID.fromString(uuidString)))
        );

        Mockito.verify(reactiveKafkaProducerTemplate, Mockito.times(0)).send(Mockito.anyString(), Mockito.any(byte[].class));
        Thread.sleep(Duration.ofSeconds(10));
    }

    @Test
    void uploadHandlerFunctionTestByWebTestClient() throws IOException {
        // Given file for testing
        ClassPathResource wavClassPathResource = new ClassPathResource("data/foo.wav");
        byte[] fileContent = wavClassPathResource.getInputStream().readAllBytes();
        var uuidString = UUID.randomUUID().toString();
        String label = "limdongjin";
        // Given cache names
        String uploadCache = "uploadCache";
        String uuid2label = "uuid2label";

        Mockito.when(reactiveKafkaProducerTemplate.send(Mockito.anyString(), Mockito.any(byte[].class)))
                .thenReturn(Mono.just(MyTestUtil.emptySenderResultVoid()));

        // Prepare MultipartBody
        BodyInserters.MultipartInserter multipartInserter = MyTestUtil.prepareMultipartInserter(wavClassPathResource, uuidString, label);

        // Execute Request and Verify Response
        WebTestClient webTestClient = WebTestClient.bindToRouterFunction(new SpeechRouter().speechRoute(speechUploadHandler))
                .configureClient()
                .build();
        EntityExchangeResult<byte[]> entityExchangeResult = webTestClient
                .post().uri("/api/speech/upload")
                .accept(MediaType.MULTIPART_FORM_DATA)
                .body(multipartInserter)

                .exchange()
                // Verify Response Format
                .expectBody()
                .jsonPath("$.userName").isEqualTo(uuidString)
                .jsonPath("$.message").exists()
                .returnResult();

        // must request contains file blob content
        assertTrue(
            new String(entityExchangeResult.getRequestBodyContent())
                    .contains(new String(fileContent))
        );

        // must send event to kafka topic
        Mockito.verify(reactiveKafkaProducerTemplate, Mockito.times(1)).send(Mockito.eq("user-pending"), Mockito.any(byte[].class));

        // must store blob to ignite
        Assertions.assertEquals(1, igniteRepository.size(uploadCache));
        assertEquals(new String(fileContent), new String(igniteRepository.<UUID, byte[]>get(uploadCache, UUID.fromString(uuidString))));

        assertEquals(1, igniteRepository.size(uuid2label));
        assertEquals(label, igniteRepository.<UUID, String>get(uuid2label, UUID.fromString(uuidString)));

        assertEquals(0, igniteRepository.size("authCache"));
    }

    @Test
    void registerHandlerFunctionTestByWebTestClient() throws IOException, InterruptedException {
        // Given file for testing
        ClassPathResource wavClassPathResource = new ClassPathResource("data/foo.wav");
        byte[] fileContent = wavClassPathResource.getInputStream().readAllBytes();
        var uuidString = UUID.randomUUID().toString();
        String label = "ignite";
        // Given cache names
        String uploadCache = "uploadCache";
        String authCache = "authCache";
        String uuid2label = "uuid2label";

        // Prepare MultipartBody
        BodyInserters.MultipartInserter multipartInserter = MyTestUtil.prepareMultipartInserter(wavClassPathResource, uuidString, label);

        // Execute Request and Verify Response
        WebTestClient webTestClient = WebTestClient.bindToRouterFunction(new SpeechRouter().speechRoute(speechUploadHandler))
                .configureClient()
                .build();
        EntityExchangeResult<byte[]> entityExchangeResult = webTestClient
                .post().uri("/api/speech/register")
                .accept(MediaType.MULTIPART_FORM_DATA)
                .body(multipartInserter)
                .exchange()
                // Verify Response Format
                .expectBody()
                .jsonPath("$.userName").isEqualTo(uuidString)
                .jsonPath("$.message").exists()
                .returnResult();

        // must request contains file blob content
        assertTrue(
                new String(Objects.<byte[]>requireNonNull(entityExchangeResult.getRequestBodyContent()))
                        .contains(new String(fileContent))
        );

        // never send event to kafka topic
        Mockito.verify(reactiveKafkaProducerTemplate, Mockito.never()).send(Mockito.eq("user-pending"), Mockito.any(byte[].class));

        // must store blob to ignite cache
        Assertions.assertEquals(1, igniteRepository.size(uploadCache));
        assertEquals(new String(fileContent), new String(igniteRepository.<UUID, byte[]>get(uploadCache, UUID.fromString(uuidString))));

        // must store registered uuid to ignite cache
        assertEquals(1, igniteRepository.size(authCache));
        assertEquals(UUID.fromString(uuidString), igniteRepository.get(authCache, UUID.fromString(uuidString)));

        assertEquals(1, igniteRepository.size(uuid2label));
        assertEquals(label, igniteRepository.<UUID, String>get(uuid2label, UUID.fromString(uuidString)));
    }
}