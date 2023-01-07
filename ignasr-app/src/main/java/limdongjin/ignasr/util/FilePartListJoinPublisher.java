package limdongjin.ignasr.util;

import limdongjin.ignasr.handler.SpeechUploadHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;

import org.springframework.http.codec.multipart.Part;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class FilePartListJoinPublisher {
    public static Mono<byte[]> entryToBytes(Map.Entry<String, List<Part>> entry) {
        System.out.println("entryToBytes");
        System.out.println(entry.getValue().size());

        Part part = entry.getValue().get(0);

        return DataBufferUtils
                .join(part.content())
                .map(dataBuffer -> {
                    var bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
        ;
    }

    @NotNull
    public static Flux<Map.Entry<String, List<Part>>> requestToEntryFlux(ServerRequest request) {
        return request
                .multipartData()
                .flatMapIterable(Map::entrySet);
    }

    @NotNull
    public static Mono<byte[]> entryFluxToBytesMono(Flux<Map.Entry<String, List<Part>>> entryFlux, String fieldName) {
        return entryFlux
                .filter(entry -> entry.getKey().equals(fieldName))
                .single()
                .onErrorMap(NoSuchElementException.class, SpeechUploadHandler::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, SpeechUploadHandler::exceedBufferSizeLimit)
                .flatMap(FilePartListJoinPublisher::entryToBytes);
    }
}
