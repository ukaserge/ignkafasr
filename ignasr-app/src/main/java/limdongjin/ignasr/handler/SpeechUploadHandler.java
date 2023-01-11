package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.SpeechUploadResponseDto;
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
    public static String allowedOrigin;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;
    private final IgniteRepository igniteRepository;

    public SpeechUploadHandler(IgniteRepository igniteRepository, ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate){
        this.igniteRepository = igniteRepository;
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
    }
    public Mono<ServerResponse> index(ServerRequest request){
        return ok().bodyValue("hello world");
    }
    public Mono<ServerResponse> upload(ServerRequest request){
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(request);

        Mono<UUID> uuidMono = fieldNameToBytesMono.apply("name").map(s -> UUID.fromString(new String(s)));
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
        Mono<String> labelMono = fieldNameToBytesMono.apply("label").map(String::new);

        Mono<Mono<UUID>> fileUploadMonoMono = Mono.zip(uuidMono, fileMono)
                .flatMap(uuid2file -> igniteRepository.putAsync("uploadCache", uuid2file.getT1(), uuid2file.getT2()))
        ;

        Mono<Mono<UUID>> labelUploadMonoMono = Mono.zip(labelMono, uuidMono)
                .flatMap(label2uuid -> igniteRepository.putAsync("uuid2label", label2uuid.getT2(), label2uuid.getT1()))
        ;

        return Mono.zip(fileUploadMonoMono, labelUploadMonoMono, Mono.just(labelMono))
                .flatMap(uuid2label -> {
                    Mono<UUID> authUploadMono = uuid2label.getT1();
                    Mono<UUID> labelUploadMono = uuid2label.getT2();
                    Mono<String> labelMonoo = uuid2label.getT3();

                    return Mono.zip(authUploadMono, labelUploadMono, labelMonoo)
                            .flatMap(tuple2 -> reactiveKafkaProducerTemplate.send("user-pending", tuple2.getT1().toString()).thenReturn(tuple2))
                            .flatMap(tuple2 -> toSuccessResponseDtoMono(tuple2.getT1(), "success upload; ", tuple2.getT3()));
                })
                .flatMap(responseDto -> ok().headers(SpeechUploadHandler::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
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
                .flatMap(responseDto -> ok().headers(SpeechUploadHandler::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
        ;

    }
    private static void addCorsHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
    }
    public static Mono<SpeechUploadResponseDto> toSuccessResponseDtoMono(UUID reqId, String msg, String label) {
        return Mono.just(new SpeechUploadResponseDto(reqId.toString(), String.format("%s; %s; %s", msg, reqId.toString(), label), label)) ;
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////
//        AudioRequestDto dto = AudioRequestDto.from(request);
//
//        return Mono.zip(dto.getReqIdMono(), dto.getFileMono(), dto.getLabelMono())
//                .flatMap(tuple3 -> {
//                    UUID reqId = tuple3.getT1();
//                    byte[] file = tuple3.getT2();
//                    String label = tuple3.getT3();
//
//                    igniteRepository.put("uuid2label", reqId, label);
//                    igniteRepository.put("authCache", reqId, reqId);
//                    igniteRepository.put("uploadCache", reqId, file);
//
//                    Mono<SpeechUploadResponseDto> speechUploadResponseDtoMono = toSuccessResponseDtoMono(reqId, "success register; ", label);
//                    return speechUploadResponseDtoMono;
//                })
//                .flatMap(responseDto -> ok()
//                        .headers(SpeechUploadHandler::addCorsHeaders)
//                        .body(Mono.just(responseDto), SpeechUploadResponseDto.class)
//                )
//        ;
//        return reqIdMono
//                 register blob to ignite cache
//                .zipWith(labelMono, (reqId, label) -> igniteRepository.<UUID, String>put("uuid2label", reqId, label))
//                .zipWith(Mono.just("unused"), (reqId, unused) -> igniteRepository.put("authCache", reqId, reqId))
//                .zipWith(fileMono, (reqId, file) -> igniteRepository.<UUID, byte[]>put("uploadCache", reqId, file))
//                 build response
//                .zipWith(labelMono, (reqId, label) -> toSuccessResponseDtoMono(reqId, "success register; ", label))
//                .flatMap(resDtoMono -> ok()
//                        .headers(SpeechUploadHandler::addCorsHeaders)
//                        .body(resDtoMono, SpeechUploadResponseDto.class)
//                        .log()
//                )
//        ;
//        return Mono.zip(dto.getReqIdMono(), dto.getFileMono(), dto.getLabelMono())
//                .flatMap(tuple3 -> {
//                    UUID reqId = tuple3.getT1();
//                    byte[] file = tuple3.getT2();
//                    String label = tuple3.getT3();
//
//                    igniteRepository.put("uuid2label", reqId, label);
//                    igniteRepository.put("uploadCache", reqId, file);
//
//                    Mono<SenderResult<Void>> senderResultMono = reactiveKafkaProducerTemplate.send("user-pending", reqId.toString());
//                    Mono<SpeechUploadResponseDto> speechUploadResponseDtoMono = toSuccessResponseDtoMono(reqId, "success upload; ", label);
//
//                    return senderResultMono.then(speechUploadResponseDtoMono);
//                })
//                .flatMap(responseDto -> ok()
//                        .headers(SpeechUploadHandler::addCorsHeaders)
//                        .body(Mono.just(responseDto), SpeechUploadResponseDto.class)
//                )
//        ;
//            .zipWith(uuidMono, (label, uuid) -> {
//                    igniteRepository.putAsync("uuid2label", uuid, label);
//                    return label;
//                })
//                .publishOn(Schedulers.boundedElastic())
//        return Mono.zip(uploadMono, labelMono)
//                .publishOn(Schedulers.boundedElastic())
//                .flatMap(tuple -> igniteRepository.putAsync("authCache", tuple.getT1(), tuple.getT1()))
//                .flatMap(tuple -> toSuccessResponseDtoMono(tuple.getT1(), "success register; ", tuple.getT2()))
//                .flatMap(responseDto -> ok().headers(SpeechUploadHandler::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
//        ;
//        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(request);
//
//        Mono<UUID> uuidMono = fieldNameToBytesMono.apply("name")
//                .map(s -> UUID.fromString(new String(s)));
//
//        Mono<UUID> uploadMono = fieldNameToBytesMono.apply("file")
//                .subscribeOn(Schedulers.boundedElastic())
//                .zipWith(uuidMono, (file, uuid) -> igniteRepository.put("uploadCache", uuid, file))
//                .publishOn(Schedulers.boundedElastic())
//        ;
//
//        Mono<String> labelMono = fieldNameToBytesMono.apply("label")
//                .map(String::new)
//                .zipWith(uuidMono, (label, uuid) -> {
//                    igniteRepository.put("uuid2label", uuid, label);
//                    return label;
//                })
//                .publishOn(Schedulers.boundedElastic())
//        ;
//
//        return Mono.zip(uploadMono, labelMono)
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMap(tuple -> reactiveKafkaProducerTemplate.send("user-pending", tuple.getT1().toString()).thenReturn(tuple))
//                .flatMap(tuple -> toSuccessResponseDtoMono(tuple.getT1(), "success upload; ", tuple.getT2()))
//                .flatMap(responseDto -> ok().headers(SpeechUploadHandler::addCorsHeaders).body(Mono.just(responseDto), SpeechUploadResponseDto.class))
//                .publishOn(Schedulers.boundedElastic())
//        ;
