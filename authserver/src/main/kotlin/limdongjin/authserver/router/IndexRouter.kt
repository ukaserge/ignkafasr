package limdongjin.authserver.router

import limdongjin.authserver.handler.IndexHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class IndexRouter {
    @Bean
    fun indexRoute(handler: IndexHandler): RouterFunction<ServerResponse> = router {
        GET("/", handler::index)
    }
}