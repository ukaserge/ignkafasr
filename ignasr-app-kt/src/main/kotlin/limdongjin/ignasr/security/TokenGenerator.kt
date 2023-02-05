package limdongjin.ignasr.security

import io.jsonwebtoken.JwtBuilder
import org.springframework.security.core.GrantedAuthority

interface TokenGenerator {
    fun signedBuilder(): JwtBuilder
    fun generate(subject: String, authorities: Collection<GrantedAuthority>): String
}