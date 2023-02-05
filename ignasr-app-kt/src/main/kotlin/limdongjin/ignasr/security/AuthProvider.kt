package limdongjin.ignasr.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono

interface AuthProvider {
    fun extractAuthentication(token: String): Mono<UsernamePasswordAuthenticationToken>
}