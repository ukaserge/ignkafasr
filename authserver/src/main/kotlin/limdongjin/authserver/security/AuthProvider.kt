package limdongjin.authserver.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.core.publisher.Mono

interface AuthProvider {
    fun extractAuthentication(token: String): Mono<UsernamePasswordAuthenticationToken>
}