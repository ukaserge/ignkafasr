package limdongjin.authserver.handler

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class IndexHandler {
    fun index(request: ServerRequest): Mono<ServerResponse> {
        return ok().bodyValue("hello world!!")
    }
}