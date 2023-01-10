package limdongjin.ignasr.dto;

import limdongjin.ignasr.util.MultiPartUtil;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

public class AudioRequestDto {
    private Mono<UUID> reqIdMono;
    private Mono<byte[]> fileMono;
    public Mono<String> labelMono;

    public AudioRequestDto() {
    }

    public AudioRequestDto(Mono<UUID> reqIdMono, Mono<byte[]> fileMono, Mono<String> labelMono) {
        this.reqIdMono = reqIdMono;
        this.fileMono = fileMono;
        this.labelMono = labelMono;
    }

    public static AudioRequestDto from(ServerRequest serverRequest) {
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(serverRequest);

        Mono<UUID> uuidMono = fieldNameToBytesMono.apply("name").map(String::new).map(UUID::fromString);
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
        Mono<String> labelMono = fieldNameToBytesMono.apply("label").map(String::new);

        return new AudioRequestDto(uuidMono, fileMono, labelMono);
    }

    /** getter and setter **/
    public Mono<UUID> getReqIdMono() {
        return reqIdMono;
    }

    public void setReqIdMono(Mono<UUID> reqIdMono) {
        this.reqIdMono = reqIdMono;
    }

    public Mono<byte[]> getFileMono() {
        return fileMono;
    }

    public void setFileMono(Mono<byte[]> fileMono) {
        this.fileMono = fileMono;
    }

    public Mono<String> getLabelMono() {
        return labelMono;
    }

    public void setLabelMono(Mono<String> labelMono) {
        this.labelMono = labelMono;
    }
}
