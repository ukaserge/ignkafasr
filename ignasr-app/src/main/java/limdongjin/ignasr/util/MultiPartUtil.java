package limdongjin.ignasr.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.InsufficientResourcesException;
import javax.naming.LimitExceededException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class MultiPartUtil {
    public static Function<String, Mono<byte[]>> toFunctionThatFieldNameToBytesMono(ServerRequest request) {
        Flux<Map.Entry<String, List<Part>>> entryFlux = request.multipartData().flatMapIterable(Map::entrySet);
        return (String fieldName) -> MultiPartUtil.toBytesMono(entryFlux, fieldName);
    }
    @NotNull
    public static Flux<Map.Entry<String, List<Part>>> toEntryPartsFlux(ServerRequest request) {
        return request
                .multipartData()
                .flatMapIterable(Map::entrySet);
    }

    @NotNull
    public static Mono<byte[]> toBytesMono(Flux<Map.Entry<String, List<Part>>> entryPartsFlux, String fieldName) {
        return entryPartsFlux
                .filter(entry -> entry.getKey().equals(fieldName))
                .single()
                .onErrorMap(NoSuchElementException.class, MultiPartUtil::missingRequiredFields)
                .onErrorMap(DataBufferLimitException.class, MultiPartUtil::exceedBufferSizeLimit)
                .flatMap(MultiPartUtil::toBytesMono);
    }

    public static Mono<byte[]> toBytesMono(Map.Entry<String, List<Part>> entry) {
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
                });
    }

    public static Throwable missingRequiredFields(NoSuchElementException throwable) {
        throwable.printStackTrace();
        return new InsufficientResourcesException("required field(s) is missing ");
    }

    public static Throwable exceedBufferSizeLimit(DataBufferLimitException throwable) {
        throwable.printStackTrace();
        return new LimitExceededException("Part exceeded the disk usage limit ");
    }
}
