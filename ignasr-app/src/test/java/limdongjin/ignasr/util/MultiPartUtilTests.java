package limdongjin.ignasr.util;

import limdongjin.ignasr.MyTestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

class MultiPartUtilTests {
    @Test
    void canLoadWavBlobFromServerRequest() throws IOException, UnsupportedAudioFileException {
        // Given
        var name = UUID.randomUUID().toString();
        ClassPathResource classPathResource = new ClassPathResource("data/foo.wav");
        File file = classPathResource.getFile();
        AudioInputStream expectedAudioInputStream = AudioSystem.getAudioInputStream(file);

        // Prepare ServerRequest
        ServerRequest serverRequest = MyTestUtil.prepareServerRequest(classPathResource, name);

        // Extract file, name from ServerRequest, Load Audio
        Function<String, Mono<byte[]>> fieldNameToBytesMono = MultiPartUtil.toFunctionThatFieldNameToBytesMono(serverRequest);
        Mono<byte[]> fileMono = fieldNameToBytesMono.apply("file");
        Mono<String> nameMono = fieldNameToBytesMono.apply("name").map(String::new);

        AudioInputStream actualAudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(fileMono.block()));

        // Verify
        Assertions.assertEquals(name, nameMono.block());
        Assertions.assertEquals(expectedAudioInputStream.getFormat().toString(), actualAudioInputStream.getFormat().toString());
        Assertions.assertEquals(expectedAudioInputStream.getFrameLength(), actualAudioInputStream.getFrameLength());

        System.out.println(actualAudioInputStream.getFormat());
        System.out.println(nameMono.block());
    }
}