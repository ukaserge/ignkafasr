package limdongjin.ignasr.security

import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.function.Function

class JwtAuthConverter(
    private val authProvider: AuthProvider
): Function<ServerWebExchange, Mono<Authentication>> {
    override fun apply(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.justOrEmpty(exchange)
            .mapNotNull { exc -> exc.request.headers.getFirst(HttpHeaders.AUTHORIZATION) }
            .filter { headerValue -> headerValue != null && StringUtils.hasText(headerValue) && headerValue.startsWith(BEARER_PREFIX) }
            .mapNotNull { headerValue -> headerValue!!.substring(7) }
            .flatMap { token -> authProvider.extractAuthentication(token) }
            .switchIfEmpty(Mono.empty())
            .map { it as Authentication }
    }
    companion object {
        private val BEARER_PREFIX = "Bearer "
    }
}