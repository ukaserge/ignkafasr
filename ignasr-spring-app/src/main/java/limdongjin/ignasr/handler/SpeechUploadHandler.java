package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.SpeechUploadResponseDto;
import limdongjin.ignasr.event.IgnasrEvent;
import limdongjin.ignasr.util.FilePartListJoinPublisher;
import org.apache.ignite.IgniteClientDisconnectedException;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientConnectionException;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.Part;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.InsufficientResourcesException;
import javax.naming.LimitExceededException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class SpeechUploadHandler {
    public ClientConfiguration clientConfiguration;
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;
    public SpeechUploadHandler(ClientConfiguration clientConfiguration, ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate){
        this.clientConfiguration = clientConfiguration;
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
    }

    public Mono<ServerResponse> index(ServerRequest request){
        return ok().bodyValue("hello world");
    }

    public Mono<ServerResponse> upload(ServerRequest request){
        var client = Ignition.startClient(clientConfiguration);

//        Mono<UUID> requestUuidMono = Mono.just(UUID.randomUUID());

        // :: request -> entryFlux
        Flux<Map.Entry<String, List<Part>>> entryFlux = request
                .multipartData()
                .flatMapIterable(Map::entrySet)
        ;

        // :: entryFlux -> uuidMono
        Mono<UUID> uuidMono = entryFlux
                .filter(entry -> entry.getKey().equals("name"))
                .single()
                .onErrorMap(NoSuchElementException.class, SpeechUploadHandler::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, SpeechUploadHandler::exceedBufferSizeLimit)
                .flatMap(FilePartListJoinPublisher::entryToBytes)
                .map(String::new)
                .map(UUID::fromString)
        ;

        // :: entryFlux -> bytesMono
        Mono<byte[]> bytesMono = entryFlux
                .filter(entry -> entry.getKey().equals("file"))
                .single()
                .onErrorMap(NoSuchElementException.class, SpeechUploadHandler::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, SpeechUploadHandler::exceedBufferSizeLimit)
                .flatMap(FilePartListJoinPublisher::entryToBytes)
        ;

        return uuidMono
                .zipWith(bytesMono)
                .flatMap(uuid2bytes -> {
                    ClientCache<UUID, byte[]> uploadCache = client.getOrCreateCache("uploadCache");

                    uploadCache.put(uuid2bytes.getT1(), uuid2bytes.getT2());
                    return Mono.just(uuid2bytes.getT1());
                })
                .doOnNext(this::updateStateToPendingUpload)
                .flatMap(uuid -> Mono.just(new SpeechUploadResponseDto(uuid.toString(), "success upload; "+uuid.toString())))
                .flatMap(resDto -> ok()
                        .headers(httpHeaders -> {
                            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://kafasr.limdongjin.com");
                            httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
                        })
                        .body(Mono.just(resDto), SpeechUploadResponseDto.class)
                        .log()
                )
                .doOnNext(unused -> client.close())
        ;
    }
    public Mono<ServerResponse> register(ServerRequest request){
        var client = Ignition.startClient(clientConfiguration);

//        Mono<UUID> requestUuidMono = Mono.just(UUID.randomUUID());

        // :: request -> entryFlux
        Flux<Map.Entry<String, List<Part>>> entryFlux = request
                .multipartData()
                .flatMapIterable(Map::entrySet)
        ;

        // :: entryFlux -> uuidMono
        Mono<UUID> uuidMono = entryFlux
                .filter(entry -> entry.getKey().equals("name"))
                .single()
                .onErrorMap(NoSuchElementException.class, SpeechUploadHandler::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, SpeechUploadHandler::exceedBufferSizeLimit)
                .flatMap(FilePartListJoinPublisher::entryToBytes)
                .map(String::new)
                .map(UUID::fromString)
                ;

        // :: entryFlux -> bytesMono
        Mono<byte[]> bytesMono = entryFlux
                .filter(entry -> entry.getKey().equals("file"))
                .single()
                .onErrorMap(NoSuchElementException.class, SpeechUploadHandler::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, SpeechUploadHandler::exceedBufferSizeLimit)
                .flatMap(FilePartListJoinPublisher::entryToBytes)
                ;

        return uuidMono
                .zipWith(bytesMono)
                .flatMap(uuid2bytes -> {
                    ClientCache<UUID, byte[]> uploadCache = client.getOrCreateCache("uploadCache");
                    uploadCache.put(uuid2bytes.getT1(), uuid2bytes.getT2());
                    return Mono.just(uuid2bytes.getT1());
                })
                .flatMap(uuid -> {
                    ClientCache<UUID, UUID> authCache = client.getOrCreateCache("authCache");
                    authCache.put(uuid, uuid);
                    return Mono.just(uuid);
                })
                .flatMap(uuid -> Mono.just(new SpeechUploadResponseDto(uuid.toString(), "success register; "+uuid.toString())))
                .flatMap(resDto -> ok()
                        .headers(httpHeaders -> {
                            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
                            httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
                        })
                        .body(Mono.just(resDto), SpeechUploadResponseDto.class)
                        .log()
                )
                .doOnNext(unused -> client.close())
                ;
    }
    private UUID sendToIgniteCache(UUID reqUuid, byte[] bytes) {
        System.out.println(reqUuid);
        try (var client = Ignition.startClient(clientConfiguration)) {
            ClientCache<UUID, byte[]> uploadCache = client.getOrCreateCache("uploadCache");

            uploadCache.put(reqUuid, bytes);
            return reqUuid;
        } catch (IgniteClientDisconnectedException e) {
            if (e.getCause() instanceof IgniteClientDisconnectedException) {
                IgniteClientDisconnectedException cause = (IgniteClientDisconnectedException) e.getCause();
                cause.reconnectFuture().get(); // Wait until the client is reconnected.
                var client = Ignition.startClient(clientConfiguration);
                ClientCache<UUID, byte[]> uploadCache = client.getOrCreateCache("uploadCache");
            }
        }

        throw new RuntimeException();
    }

    private UUID updateStateToPendingUpload(UUID reqUuid) {
        // send to kafka topic
        reactiveKafkaProducerTemplate.send("user-pending", reqUuid.toString())
                // new IgnasrEvent(userName, reqUuid, "pending", "pending"))
                .subscribe();
        return reqUuid;
    }

    private UUID updateStateToSuccessUpload(UUID reqUuid, String userName) {
        // send to kafka topic
        reactiveKafkaProducerTemplate.send("user-successupload", reqUuid.toString())
                .subscribe();

        return reqUuid;
    }
    private static Throwable missingRequiredFields(NoSuchElementException throwable) {
        throwable.printStackTrace();
        return new InsufficientResourcesException("required field(s) is missing ");
    }

    private static Throwable exceedBufferSizeLimit(DataBufferLimitException throwable) {
        throwable.printStackTrace();
        return new LimitExceededException("Part exceeded the disk usage limit ");
    }

    public ReactiveKafkaProducerTemplate<String, String> getReactiveKafkaProducerTemplate() {
        return reactiveKafkaProducerTemplate;
    }

    public void setClientConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public void setReactiveKafkaProducerTemplate(ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate) {
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
    }

    public ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }
}
