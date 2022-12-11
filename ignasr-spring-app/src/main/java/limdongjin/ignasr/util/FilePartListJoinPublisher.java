package limdongjin.ignasr.util;

import io.netty.util.ReferenceCountUtil;
import org.springframework.core.io.buffer.DataBufferUtils;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class FilePartListJoinPublisher {
    public static Mono<byte[]> entryToBytes(Map.Entry<String, List<Part>> entry) {
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
}
