package limdongjin.ignasr;

import limdongjin.ignasr.repository.IgniteRepositoryImpl;
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
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderResult;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MyTestUtil {
    private static MultipartBodyBuilder prepareMultipartBodyBuilder(ClassPathResource fileFieldValue, String nameFieldValue, String labelFieldValue, String userIdFieldValue){
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", fileFieldValue);
        multipartBodyBuilder.part("name", nameFieldValue);
        multipartBodyBuilder.part("label", labelFieldValue);
        multipartBodyBuilder.part("userId", userIdFieldValue);

        return multipartBodyBuilder;
    }

    public static BodyInserters.MultipartInserter prepareMultipartInserter(ClassPathResource fileFieldValue, String reqIdFieldValue, String labelFieldValue, String userIdFieldValue) {
        MultipartBodyBuilder multipartBodyBuilder = prepareMultipartBodyBuilder(fileFieldValue, reqIdFieldValue, labelFieldValue, userIdFieldValue);
        return BodyInserters.fromMultipartData(multipartBodyBuilder.build());
    }

    public static ServerRequest prepareServerRequest(ClassPathResource fileFieldValue, String nameFieldValue, String labelFieldValue, String userIdFieldValue) {
        MultipartBodyBuilder multipartBodyBuilder = prepareMultipartBodyBuilder(fileFieldValue, nameFieldValue, labelFieldValue, userIdFieldValue);
        MockClientHttpRequest outputMessage = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));

        Mono<Void> writeMono = new MultipartHttpMessageWriter()
                .write(Mono.just(multipartBodyBuilder.build()), null, MediaType.MULTIPART_FORM_DATA, outputMessage, null)
        ;

        return writeMono
                .then(Mono.just(1))
                .map(unused -> {
                    MockServerHttpRequest request = MockServerHttpRequest
                            .method(HttpMethod.POST, "http://test.com")
                            .accept(MediaType.MULTIPART_FORM_DATA)
                            .contentType(Objects.<MediaType>requireNonNull(outputMessage.getHeaders().getContentType()))
                            .body(outputMessage.getBody());
                    return ServerRequest.create(MockServerWebExchange.from(request), Collections.emptyList());
                })
                .block(Duration.ofSeconds(5))
        ;
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

    public static <K, V> ConcurrentHashMap<K, V> stubbingIgniteRepository(
            IgniteRepositoryImpl igniteRepositoryImpl,
            String cacheName,
            boolean stubGet,
            boolean stubPut,
            boolean stubSize,
            boolean stubPutAsync,
            ConcurrentHashMap<K, V> m
    ){
        ConcurrentHashMap<K, V> igniteUploadCacheMock;
        if(m == null){
            igniteUploadCacheMock = new ConcurrentHashMap<K, V>();
        }else {
            igniteUploadCacheMock = m;
        }
//        ConcurrentHashMap<K, V> igniteUploadCacheMock = new ConcurrentHashMap<>();
        System.out.println(cacheName);
        System.out.println(igniteUploadCacheMock.hashCode());

        if(stubGet) {
            Mockito.when(igniteRepositoryImpl.get(Mockito.eq(cacheName), Mockito.<K>any()))
                    .thenAnswer(param -> {
                        System.out.println("GET");
                        System.out.println(igniteUploadCacheMock.hashCode());
                        System.out.println(igniteUploadCacheMock.size());

                        K key = param.getArgument(1);
                        return igniteUploadCacheMock.get(key);
                    });
        }
        if(stubPut) {
            Mockito.when(igniteRepositoryImpl.put(Mockito.eq(cacheName), Mockito.<K>any(), Mockito.<V>any()))
                    .thenAnswer(param -> {
                        System.out.println("PUT");
                        System.out.println(igniteUploadCacheMock.hashCode());
                        System.out.println(igniteUploadCacheMock.size());

                        K key = param.getArgument(1);
                        V value = param.getArgument(2);
                        igniteUploadCacheMock.put(key, value);
                        return key;
                    });
        }

        if(stubSize){
            Mockito.when(igniteRepositoryImpl.size(Mockito.eq(cacheName)))
                    .thenAnswer(param -> {
                        System.out.println("SIZE");
                        System.out.println(igniteUploadCacheMock.hashCode());
                        System.out.println(igniteUploadCacheMock.size());

                        return igniteUploadCacheMock.size();
                    });
        }

        if(stubPutAsync) {
            Mockito.when(igniteRepositoryImpl.putAsync(Mockito.eq(cacheName), Mockito.<K>any(), Mockito.<V>any()))
                    .thenAnswer(param -> {
                        System.out.println("PUT ASYNC");
                        System.out.println(igniteUploadCacheMock.hashCode());
                        System.out.println(igniteUploadCacheMock.size());

                        K key = param.getArgument(1);
                        V value = param.getArgument(2);

                        Mono<K> wrap = Mono.just(key)
                                .doOnNext(kk -> {
                                    System.out.println("doOnNext");
                                    System.out.println(igniteUploadCacheMock.hashCode());
                                    igniteUploadCacheMock.put(key, value);
                                    System.out.println(igniteUploadCacheMock.size());
                                });

                        return Mono.just(wrap);
                    })
            ;
        }


        return igniteUploadCacheMock;
    }
}
