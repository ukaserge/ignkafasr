package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.SpeechUploadResponseDto;
import limdongjin.ignasr.protos.UserPendingProto;
import limdongjin.ignasr.repository.IgniteRepository;
import limdongjin.ignasr.util.MultiPartUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class SpeechUploadHandler {
    @Value("${limdongjin.ignasr.cors.origin}")
    public String allowedOrigin;

    private final ReactiveKafkaProducerTemplate<String, byte[]> reactiveKafkaProducerTemplate;
    private final IgniteRepository igniteRepository;

    public SpeechUploadHandler(IgniteRepository igniteRepository, ReactiveKafkaProducerTemplate<String, byte[]> reactiveKafkaProducerTemplate){
        this.igniteRepository = igniteRepository;
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
    }
    public Mono<ServerResponse> index(ServerRequest request){
        return ok().bodyValue("hello world");
    }
    public Mono<ServerResponse> upload(ServerRequest request){
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(request);

        Mono<UUID> userIdMono = fieldNameToBytesMono.apply("userId").map(s -> UUID.fromString(new String(s)));
        Mono<UUID> reqIdMono = fieldNameToBytesMono.apply("name").map(s -> UUID.fromString(new String(s)));
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
//        Mono<String> labelMono = fieldNameToBytesMono.apply("label").map(String::new);

        Mono<Mono<UUID>> fileUploadMonoMono = Mono.zip(reqIdMono, fileMono)
                .flatMap(reqId2file -> igniteRepository.putAsync("uploadCache", reqId2file.getT1(), reqId2file.getT2()))
        ;

        Mono<Mono<UUID>> userIdUploadMonoMono = Mono.zip(reqIdMono, userIdMono)
                .flatMap(reqId2userId -> igniteRepository.putAsync("reqId2userId", reqId2userId.getT1(), reqId2userId.getT2()))
        ;
        
        // TODO refactoring
        return Mono.zip(fileUploadMonoMono, userIdUploadMonoMono, Mono.just(userIdMono))
                .flatMap(monoTuple3 -> {
                    Mono<UUID> fileUploadMono = monoTuple3.getT1();
                    Mono<UUID> userIdUploadMono = monoTuple3.getT2();
                    Mono<UUID> userIdMonoJust = monoTuple3.getT3();
//                    Mono<String> labelMonoJust = monoTuple3.getT4();

                    return Mono.zip(fileUploadMono, userIdUploadMono, userIdMonoJust)
                            .flatMap(tuple3 -> {
                                var reqId = tuple3.getT1().toString();
                                var userId = tuple3.getT3().toString();
                                byte[] reqIdBytes = UserPendingProto.UserPending
                                        .newBuilder()
                                        .setReqId(reqId)
                                        .setUserId(userId)
                                        .build()
                                        .toByteArray();

                                return reactiveKafkaProducerTemplate.send("user-pending", reqIdBytes).thenReturn(tuple3);
                            })
                            .flatMap(tuple3 -> toSuccessResponseDtoMono(tuple3.getT1(), "success upload; userId = ", tuple3.getT3().toString()));
                })
                .flatMap(responseDto -> ok().headers(this::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
        ;

    }

    public Mono<ServerResponse> register(ServerRequest request){
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(request);

        Mono<UUID> uuidMono = fieldNameToBytesMono.apply("name").map(s -> UUID.fromString(new String(s)));
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
        Mono<String> labelMono = fieldNameToBytesMono.apply("label").map(String::new).log();

        Mono<Mono<UUID>> fileUploadMonoMono = Mono.zip(uuidMono, fileMono)
                .flatMap(uuid2file -> igniteRepository.putAsync("uploadCache", uuid2file.getT1(), uuid2file.getT2()))
        ;

        Mono<Mono<UUID>> labelUploadMonoMono = Mono.zip(labelMono, uuidMono)
                .flatMap(label2uuid -> igniteRepository.putAsync("uuid2label", label2uuid.getT2(), label2uuid.getT1()))
        ;

        Mono<Mono<UUID>> authUploadMonoMono = uuidMono
                .flatMap(uuid -> igniteRepository.putAsync("authCache", uuid, uuid))
        ;

        return Mono.zip(authUploadMonoMono, fileUploadMonoMono, labelUploadMonoMono, Mono.just(labelMono))
                .flatMap(t -> {
                    Mono<UUID> authUploadMono = t.getT1();
                    Mono<UUID> fileUploadMono = t.getT2();
                    Mono<UUID> labelUploadMono = t.getT3();
                    Mono<String> labelMonoo = t.getT4();

                    return Mono.zip(authUploadMono, fileUploadMono, labelUploadMono, labelMonoo)
                            .flatMap(tuple3 -> toSuccessResponseDtoMono(tuple3.getT1(), "success register; ", tuple3.getT4()));
                })
                .flatMap(responseDto -> ok().headers(this::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
        ;

    }
    
    private void addCorsHeaders(HttpHeaders httpHeaders) {
        if(allowedOrigin == null || allowedOrigin.equals("*")){
            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
        }else{
            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        }
        httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
    }

    public static Mono<SpeechUploadResponseDto> toSuccessResponseDtoMono(UUID reqId, String msg, String label) {
        return Mono.just(new SpeechUploadResponseDto(reqId.toString(), String.format("%s; %s; %s", msg, reqId.toString(), label), label)) ;
    }
}
