package limdongjin.ignasr.handler;

import limdongjin.ignasr.dto.SpeechUploadResponseDto;
import limdongjin.ignasr.protos.UserPendingProto;
import limdongjin.ignasr.repository.IgniteRepository;
import limdongjin.ignasr.util.MultiPartUtil;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Time;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.kafka.sender.internals.DefaultKafkaSender;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import java.util.Random;

@Component
public class SpeechUploadHandler {
    @Value("${limdongjin.ignasr.cors.origin}")
    public String allowedOrigin;

    private final ReactiveKafkaProducerTemplate<String, byte[]> reactiveKafkaProducerTemplate;
    private final IgniteRepository igniteRepository;

    private final static Logger logger = Loggers.getLogger(SpeechUploadHandler.class);

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

        Mono<Mono<UUID>> fileUploadMonoMono = Mono.zip(reqIdMono, fileMono)
                .flatMap(reqId2file -> igniteRepository.putAsync("uploadCache", reqId2file.getT1(), reqId2file.getT2()))
        ;

        Mono<Mono<UUID>> userIdUploadMonoMono = Mono.zip(reqIdMono, userIdMono)
                .flatMap(reqId2userId -> igniteRepository.putAsync("reqId2userId", reqId2userId.getT1(), reqId2userId.getT2()))
        ;

        Mono<Tuple2<UUID, UUID>> kafkaProduceMono = Mono.zip(reqIdMono, userIdMono)
                .flatMap(tuple2 -> {
                    byte[] userPendingBytes = UserPendingProto.UserPending
                            .newBuilder()
                            .setReqId(tuple2.getT1().toString())
                            .setUserId(tuple2.getT2().toString())
                            .build()
                            .toByteArray();
                    return reactiveKafkaProducerTemplate.send(SenderRecord.create("user-pending", new Random().nextInt(10), Time.SYSTEM.milliseconds(), null, userPendingBytes, null))
                            .retry(2)
                            .flatMap(unused -> Mono.just(tuple2));
                });
        return Mono.zip(fileUploadMonoMono, userIdUploadMonoMono)
                .flatMap(tuple2 -> Mono.zip(tuple2.getT1(), tuple2.getT2()).flatMap(t -> kafkaProduceMono))
                .flatMap(tuple2 -> toSuccessResponseDtoMono(tuple2.getT1(), "success upload; userId = ", tuple2.getT2().toString()))
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
