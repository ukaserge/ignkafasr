package limdongjin.authserver.security

import io.jsonwebtoken.JwtBuilder
import org.springframework.security.core.GrantedAuthority

interface TokenGenerator {
    fun signedBuilder(): JwtBuilder
    fun generate(subject: String, authorities: Collection<GrantedAuthority>, isRefresh: Boolean = false): String
}