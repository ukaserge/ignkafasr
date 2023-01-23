package limdongjin.ignasr.util;

import limdongjin.ignasr.MyTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

class MultiPartUtilTests {
    @Test
    void canLoadWavBlobFromServerRequest() throws IOException, UnsupportedAudioFileException {
        // Given
        var reqId = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();
        ClassPathResource classPathResource = new ClassPathResource("data/foo.wav");
        File file = classPathResource.getFile();
        AudioInputStream expectedAudioInputStream = AudioSystem.getAudioInputStream(file);
        String label = "ldj";

        // Prepare ServerRequest
        ServerRequest serverRequest = MyTestUtil.prepareServerRequest(classPathResource, reqId, label, userId);

        // Extract file, name from ServerRequest, Load Audio
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(serverRequest);
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
        Mono<String> nameMono = fieldNameToBytesMono.apply("name").map(String::new);

        AudioInputStream actualAudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(fileMono.block()));

        // Verify
        Assertions.assertEquals(reqId, nameMono.block());
        Assertions.assertEquals(expectedAudioInputStream.getFormat().toString(), actualAudioInputStream.getFormat().toString());
        Assertions.assertEquals(expectedAudioInputStream.getFrameLength(), actualAudioInputStream.getFrameLength());

        System.out.println(actualAudioInputStream.getFormat());
        // example:
        // PCM_SIGNED 16000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian

        System.out.println(nameMono.block());
    }
}