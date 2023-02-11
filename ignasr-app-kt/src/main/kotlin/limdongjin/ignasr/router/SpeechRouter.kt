package limdongjin.ignasr.router

import limdongjin.ignasr.handler.SpeechUploadHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import javax.naming.InsufficientResourcesException
import javax.naming.LimitExceededException

@Configuration
class SpeechRouter {
    @Bean
    fun speechRoute(handler: SpeechUploadHandler): RouterFunction<ServerResponse> = router {
        onError<InsufficientResourcesException> { t, _ -> ServerResponse.badRequest().bodyValue(t.message!!) }
        onError<LimitExceededException> { t, _ -> ServerResponse.badRequest().bodyValue(t.message!!) }

        "/api".and(accept(MediaType.MULTIPART_FORM_DATA)).nest {
            POST("/speech/register", handler::register)
            POST("/speech/upload", handler::upload)
            POST("/speech/analysis", handler::analysis)
            POST("/speech/register2", handler::register2)
            GET("/user/foo", handler::foo)
        }
        GET("/").and(accept(MediaType.ALL)).invoke(handler::index)
    }
}
