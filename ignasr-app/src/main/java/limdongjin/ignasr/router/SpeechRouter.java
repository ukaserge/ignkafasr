package limdongjin.ignasr.router;

import limdongjin.ignasr.handler.SpeechUploadHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.naming.InsufficientResourcesException;
import javax.naming.LimitExceededException;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class SpeechRouter {
    @Bean
    public RouterFunction<ServerResponse> speechRoute(SpeechUploadHandler handler) {
        return route()
            .onError(InsufficientResourcesException.class, (e, req) -> ServerResponse.badRequest().bodyValue(e.getMessage()))
            .onError(LimitExceededException.class, (e, req) -> ServerResponse.badRequest().bodyValue(e.getMessage()))
            .GET(
                    "/",
                    accept(MediaType.ALL),
                    handler::index
            )
            .POST(
                    "/api/speech/upload",
                    accept(MediaType.MULTIPART_FORM_DATA),
                    handler::upload
            )
            .POST(
                "/api/speech/register",
                accept(MediaType.MULTIPART_FORM_DATA),
                handler::register
            )
            .build();
    }
}
