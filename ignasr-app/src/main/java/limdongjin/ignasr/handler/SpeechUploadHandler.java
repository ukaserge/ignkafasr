package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.SpeechUploadResponseDto;
import limdongjin.ignasr.util.FilePartListJoinPublisher;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.Part;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.naming.InsufficientResourcesException;
import javax.naming.LimitExceededException;
import java.util.*;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class SpeechUploadHandler {
    @Value("${limdongjin.ignasr.cors.origin}")
    public String allowedOrigin;

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
        // :: request -> entryFlux
        Flux<Map.Entry<String, List<Part>>> entryFlux = FilePartListJoinPublisher.requestToEntryFlux(request);

        // :: entryFlux -> uuidMono
        Mono<UUID> uuidMono = FilePartListJoinPublisher
                .entryFluxToBytesMono(entryFlux, "name")
                .map(String::new)
                .map(UUID::fromString);

        // :: entryFlux -> fileMono
        Mono<byte[]> fileMono = FilePartListJoinPublisher
                .entryFluxToBytesMono(entryFlux, "file");

        IgniteClient client = Ignition.startClient(clientConfiguration);
        return uuidMono.zipWith(fileMono)
                .flatMap(uploadToCache(client))
                .doOnNext(this::updateStateToPendingUpload)
                .flatMap(uuid -> Mono.just(new SpeechUploadResponseDto(uuid.toString(), "success upload; "+uuid.toString())))
                .flatMap(
                    resDto -> ok().headers(this::addCorsHeaders)
                        .body(Mono.just(resDto), SpeechUploadResponseDto.class)
                        .log()
                )
                .doOnNext(unused -> client.close())
        ;
    }

    public Mono<ServerResponse> register(ServerRequest request){
        // :: request -> entryFlux
        Flux<Map.Entry<String, List<Part>>> entryFlux = FilePartListJoinPublisher.requestToEntryFlux(request);

        // :: entryFlux -> uuidMono
        Mono<UUID> uuidMono = FilePartListJoinPublisher
                .entryFluxToBytesMono(entryFlux, "name")
                .map(String::new)
                .map(UUID::fromString);

        // :: entryFlux -> fileMono
        Mono<byte[]> fileMono = FilePartListJoinPublisher
                .entryFluxToBytesMono(entryFlux, "file");

        IgniteClient client = Ignition.startClient(clientConfiguration);
        return uuidMono.zipWith(fileMono)
                .flatMap(uploadToCache(client))
                .flatMap(uploadToAuthCache(client))
                .flatMap(uuid -> Mono.just(new SpeechUploadResponseDto(uuid.toString(), "success register; "+uuid.toString())))
                .flatMap(resDto -> ok()
                        .headers(this::addCorsHeaders)
                        .body(Mono.just(resDto), SpeechUploadResponseDto.class)
                        .log()
                )
                .doOnNext(unused -> client.close());
    }

    @NotNull
    private static Function<UUID, Mono<? extends UUID>> uploadToAuthCache(IgniteClient client) {
        return uuid -> {
            ClientCache<UUID, UUID> authCache = client.getOrCreateCache("authCache");
            authCache.put(uuid, uuid);
            return Mono.just(uuid);
        };
    }

    @NotNull
    private static Function<Tuple2<UUID, byte[]>, Mono<? extends UUID>> uploadToCache(IgniteClient client) {
        return uuid2bytes -> {
            ClientCache<UUID, byte[]> cache = client.getOrCreateCache("uploadCache");
            cache.put(uuid2bytes.getT1(), uuid2bytes.getT2());
            return Mono.just(uuid2bytes.getT1());
        };
    }

    private void updateStateToPendingUpload(UUID reqUuid) {
        // send to kafka topic
        reactiveKafkaProducerTemplate.send("user-pending", reqUuid.toString())
                // new IgnasrEvent(userName, reqUuid, "pending", "pending"))
                .subscribe();
    }

    private void addCorsHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
    }
    public static Throwable missingRequiredFields(NoSuchElementException throwable) {
        throwable.printStackTrace();
        return new InsufficientResourcesException("required field(s) is missing ");
    }

    public static Throwable exceedBufferSizeLimit(DataBufferLimitException throwable) {
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
