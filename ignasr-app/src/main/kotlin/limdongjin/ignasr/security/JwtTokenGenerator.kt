package limdongjin.ignasr.security

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

    override fun generate(subject: String, authorities: Collection<GrantedAuthority>): String {
        return signedBuilder()
            .setSubject(subject)
            .setIssuer(ISSUER)
            .setExpiration(Date(Date().time + 30_000))
            .claim(
                "authorities",
                authorities.parallelStream()
                    .map { (it as GrantedAuthority).authority }
                    .collect(Collectors.joining(","))
            )
            .compact()
    }

    companion object {
        private const val ISSUER = "limdongjin.ignasr"
    }
}