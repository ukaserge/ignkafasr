package limdongjin.ignasr;

import limdongjin.ignasr.repository.IgniteRepository;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class MyTestUtil {
    private static MultipartBodyBuilder prepareMultipartBodyBuilder(ClassPathResource fileFieldValue, String nameFieldValue, String labelFieldValue){
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", fileFieldValue);
        multipartBodyBuilder.part("name", nameFieldValue);
        multipartBodyBuilder.part("label", labelFieldValue);
        return multipartBodyBuilder;
    }

    public static BodyInserters.MultipartInserter prepareMultipartInserter(ClassPathResource fileFieldValue, String nameFieldValue, String labelFieldValue) {
        MultipartBodyBuilder multipartBodyBuilder = prepareMultipartBodyBuilder(fileFieldValue, nameFieldValue, labelFieldValue);
        return BodyInserters.fromMultipartData(multipartBodyBuilder.build());
    }

    public static ServerRequest prepareServerRequest(ClassPathResource fileFieldValue, String nameFieldValue, String labelFieldValue) {
        MultipartBodyBuilder multipartBodyBuilder = prepareMultipartBodyBuilder(fileFieldValue, nameFieldValue, labelFieldValue);
        MockClientHttpRequest outputMessage = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));

        new MultipartHttpMessageWriter()
                .write(Mono.just(multipartBodyBuilder.build()), null, MediaType.MULTIPART_FORM_DATA, outputMessage, null)
                .block(Duration.ofSeconds(5));

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "http://test.com")
                .accept(MediaType.MULTIPART_FORM_DATA)
                .contentType(Objects.<MediaType>requireNonNull(outputMessage.getHeaders().getContentType()))
                .body(outputMessage.getBody());

        return ServerRequest.create(MockServerWebExchange.from(request), Collections.emptyList());
    }

    public static SenderResult<Void> emptySenderResultVoid() {
        return new SenderResult<Void>() {
            @Override
            public RecordMetadata recordMetadata() {
                return null;
            }

            @Override
            public Exception exception() {
                return null;
            }

            @Override
            public Void correlationMetadata() {
                return null;
            }
        };
    }

    public static <K, V> HashMap<K, V> stubbingIgniteRepository(
            IgniteRepository igniteRepository,
            String cacheName,
            boolean stubGet,
            boolean stubPut,
            boolean stubSize
    ){
        HashMap<K, V> igniteUploadCacheMock = new HashMap<>();

        if(stubGet) {
            Mockito.when(igniteRepository.get(Mockito.eq(cacheName), Mockito.<K>any()))
                    .thenAnswer(param -> {
                        K key = param.getArgument(1);

                        return igniteUploadCacheMock.get(key);
                    });
        }
        if(stubPut) {
            Mockito.when(igniteRepository.put(Mockito.eq(cacheName), Mockito.<K>any(), Mockito.<V>any()))
                    .thenAnswer(param -> {
                        K key = param.getArgument(1);
                        V value = param.getArgument(2);

                        igniteUploadCacheMock.put(key, value);
                        return key;
                    });
        }
        if(stubSize){
            Mockito.when(igniteRepository.size(Mockito.eq(cacheName)))
                    .thenAnswer(param -> {
                        return igniteUploadCacheMock.size();
                    });
        }

        return igniteUploadCacheMock;
    }
}
