package limdongjin.authserver.router

import limdongjin.authserver.handler.AuthHandler
import limdongjin.authserver.handler.IndexHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class AuthRouter {
    @Bean
    fun authRoute(handler: AuthHandler): RouterFunction<ServerResponse> = router {
        "/api".nest {
            POST("/register", handler::register)
            POST("/login", handler::login)
            POST("/refresh", handler::refresh)
        }
    }
}