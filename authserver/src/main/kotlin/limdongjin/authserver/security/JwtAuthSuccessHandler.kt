package limdongjin.authserver.security

import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.util.Logger
import reactor.util.Loggers

class JwtAuthSuccessHandler(
    val tokenGenerator: TokenGenerator
): ServerAuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication,
    ): Mono<Void> {
        logger.info("onAuthenticationSuccess(...)")

        val exchange: ServerWebExchange = webFilterExchange.exchange
        exchange.response.headers
            .add(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${tokenGenerator.generate(authentication.name, authentication.authorities)}"
            )
        return webFilterExchange.chain.filter(exchange)
    }

    companion object {
        val logger: Logger = Loggers.getLogger(JwtAuthSuccessHandler::class.java)
    }
}