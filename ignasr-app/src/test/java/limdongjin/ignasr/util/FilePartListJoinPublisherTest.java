package limdongjin.ignasr.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class FilePartListJoinPublisherTest {

    @Test
    void entryToBytes() throws IOException, UnsupportedAudioFileException {
        // Given
        ClassPathResource classPathResource = new ClassPathResource("data/foo.wav");
        File file = classPathResource.getFile();

        AudioInputStream expectedAudioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat expectedFormat = expectedAudioInputStream.getFormat();

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", classPathResource);
        MockClientHttpRequest outputMessage = new MockClientHttpRequest(HttpMethod.POST, URI.create("/"));
        new MultipartHttpMessageWriter()
                .write(Mono.just(multipartBodyBuilder.build()), null, MediaType.MULTIPART_FORM_DATA, outputMessage, null)
                .block(Duration.ofSeconds(5));
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "http://test.com")
                .contentType(outputMessage.getHeaders().getContentType())
                .body(outputMessage.getBody());
        ServerRequest serverRequest = ServerRequest.create(MockServerWebExchange.from(request), Collections.emptyList());

        Flux<Map.Entry<String, List<Part>>> entryFlux = serverRequest.multipartData()
                .log()
                .flatMapIterable(Map::entrySet);

        Mono<byte[]> bytesMono = FilePartListJoinPublisher.entryFluxToBytesMono(entryFlux, "file");

        AudioInputStream actualAudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytesMono.block()));

        System.out.println(actualAudioInputStream.getFormat());

        Assertions.assertEquals(expectedAudioInputStream.getFormat().toString(), actualAudioInputStream.getFormat().toString());
        Assertions.assertEquals(expectedAudioInputStream.getFrameLength(), actualAudioInputStream.getFrameLength());
    }


    @Test
    void requestToEntryFlux() {
    }

    @Test
    void entryFluxToBytesMono() {
    }
}