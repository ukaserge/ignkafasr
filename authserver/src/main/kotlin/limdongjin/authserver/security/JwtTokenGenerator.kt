package limdongjin.authserver.security

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import org.springframework.security.core.GrantedAuthority
import java.util.*
import java.util.stream.Collectors

class JwtTokenGenerator(
    keyFileContent: String
): TokenGenerator {
    private val privateKey = JwtPrivateKeyLoader.loadPrivateKey(keyFileContent)

    override fun signedBuilder(): JwtBuilder {
       return Jwts.builder()
           .signWith(privateKey)
    }

    override fun generate(subject: String, authorities: Collection<GrantedAuthority>, isRefresh: Boolean): String {
        val expiration = Date(Date().time + 1000 * 60 * 10 + if(isRefresh) 1000 * 60 * 60 * 12 else 0)
        return signedBuilder()
            .setSubject(subject)
            .setIssuer(ISSUER)
            .setExpiration(expiration)
            .claim(
                "authorities",
                authorities.parallelStream()
                    .map { (it as GrantedAuthority).authority }
                    .collect(Collectors.joining(","))
            )
            .compact()
    }

    companion object {
        private const val ISSUER = "limdongjin.authserver"
    }
}