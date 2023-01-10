package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.AudioRequestDto;
import limdongjin.ignasr.dto.SpeechUploadResponseDto;
import limdongjin.ignasr.repository.IgniteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

import java.util.Arrays;
import java.util.UUID;

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
        AudioRequestDto dto = AudioRequestDto.from(request);
        var reqIdMono = dto.getReqIdMono();
        var fileMono = dto.getFileMono();
        var labelMono = dto.getLabelMono();

        Mono<UUID> uploadCacheMono = reqIdMono
                .zipWith(labelMono, (reqId, label) -> igniteRepository.<UUID, String>put("uuid2label", reqId, label))
                .zipWith(fileMono, (reqId, file) -> igniteRepository.<UUID, byte[]>put("uploadCache", reqId, file))
        ;

        Mono<SenderResult<Void>> sendResFromUserPendingTopic = reqIdMono
                .flatMap(reqId -> reactiveKafkaProducerTemplate.send("user-pending", reqId.toString()))
        ;

        Mono<ServerResponse> prepareServerResponseMono = reqIdMono
                .zipWith(labelMono, (reqId, label) -> toSuccessResponseDtoMono(reqId, "success upload; ", label))
                .flatMap(resDtoMono -> ok()
                        .headers(SpeechUploadHandler::addCorsHeaders)
                        .body(resDtoMono, SpeechUploadResponseDto.class)
                        .log()
                )
        ;

        return uploadCacheMono
                .then(sendResFromUserPendingTopic)
                .then(prepareServerResponseMono);
    }

    public Mono<ServerResponse> register(ServerRequest request){
        AudioRequestDto dto = AudioRequestDto.from(request);
        var reqIdMono = dto.getReqIdMono();
        var fileMono = dto.getFileMono();
        var labelMono = dto.getLabelMono();

        Mono<UUID> registerAndUploadMono = reqIdMono
                .zipWith(labelMono, (reqId, label) -> igniteRepository.<UUID, String>put("uuid2label", reqId, label))
                .zipWith(reqIdMono, (reqId, unused) -> igniteRepository.put("authCache", reqId, reqId))
                .zipWith(fileMono, (reqId, file) -> igniteRepository.put("uploadCache", reqId, file))
        ;

        Mono<ServerResponse> prepareServerResponseMono = reqIdMono
                .zipWith(labelMono, (reqId, label) -> toSuccessResponseDtoMono(reqId, "success register; ", label))
                .flatMap(resDtoMono -> ok()
                        .headers(SpeechUploadHandler::addCorsHeaders)
                        .body(resDtoMono, SpeechUploadResponseDto.class)
                        .log()
                )
        ;

        return registerAndUploadMono
                .then(prepareServerResponseMono);
    }

    private static void addCorsHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        httpHeaders.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList("POST", "PUT", "OPTIONS", "GET", "HEAD"));
    }
    public static Mono<SpeechUploadResponseDto> toSuccessResponseDtoMono(UUID reqId, String msg, String label) {
        return Mono.just(new SpeechUploadResponseDto(reqId.toString(), String.format("%s; %s; %s", msg, reqId.toString(), label), label)) ;
    }
}
