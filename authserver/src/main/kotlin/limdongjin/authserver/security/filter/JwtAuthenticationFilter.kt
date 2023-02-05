package limdongjin.authserver.security.filter

import limdongjin.authserver.security.AuthProvider
import limdongjin.authserver.security.CustomReactiveAuthenticationManager
import limdongjin.authserver.security.JwtAuthConverter
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.Logger
import reactor.util.Loggers
import java.util.function.Function

class JwtAuthenticationFilter(authProvider: AuthProvider): WebFilter {
    companion object {
        val logger: Logger = Loggers.getLogger(WebFilter::class.java)
    }
    val jwtAuthConverter: Function<ServerWebExchange, Mono<Authentication>> = JwtAuthConverter(authProvider)
    val customReactiveAuthenticationManager: ReactiveAuthenticationManager = CustomReactiveAuthenticationManager()
    val securityContextRepository: NoOpServerSecurityContextRepository = NoOpServerSecurityContextRepository.getInstance()
    val authSuccessHandler: ServerAuthenticationSuccessHandler = WebFilterChainServerAuthenticationSuccessHandler()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        logger.info("jwtFilter.filter(...)")

        return exchange
            .let { jwtAuthConverter.apply(it) }
            .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
            .flatMap { token ->
                val webFilterExchange = WebFilterExchange(exchange, chain)
                customReactiveAuthenticationManager.authenticate(token)
                    .flatMap { authentication -> onAuthSuccess(authentication, webFilterExchange) }
            }
    }

    private fun onAuthSuccess(authentication: Authentication, webFilterExchange: WebFilterExchange): Mono<Void> {
        logger.info("onAuthSuccess(...)")

        val exchange = webFilterExchange.exchange
        val securityContext = SecurityContextImpl()
        securityContext.authentication = authentication

        return this.securityContextRepository.save(exchange, securityContext)
            .then(this.authSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
    }
}