package limdongjin.ignasr.security

import io.jsonwebtoken.*
import io.jsonwebtoken.Jwts.parserBuilder
import org.apache.commons.codec.binary.Base64
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import reactor.core.publisher.Mono
import reactor.util.Logger
import reactor.util.Loggers
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.stream.Collectors

class JwtAuthProvider(keyFileContent: String): AuthProvider {
    companion object {
        private val logger: Logger = Loggers.getLogger(JwtAuthProvider::class.java)
        private val AUTHORITIES_KEY = "authorities"
    }
    private val privateKey = JwtPrivateKeyLoader.loadPrivateKey(keyFileContent)
    private val jwtParser: JwtParser = parserBuilder().setSigningKey(privateKey).build()

    override fun extractAuthentication(token: String): Mono<UsernamePasswordAuthenticationToken> {
        logger.debug("(JwtProvider) getAuthentication(token)")

        return extractClaims(token)
            .switchIfEmpty(Mono.empty())
            .flatMap { claims ->
                val authorities = claims[AUTHORITIES_KEY]
                    ?.let { rawAuthorities -> AuthorityUtils.commaSeparatedStringToAuthorityList(rawAuthorities.toString()) }
                    ?: AuthorityUtils.NO_AUTHORITIES

                Mono.just(CustomUserDetails(claims.subject, "masked", authorities))
            }
            .flatMap { principal ->
                Mono.just(UsernamePasswordAuthenticationToken(principal, token, principal.authorities))
            }
    }

    private fun extractClaims(token: String): Mono<Claims> {
        return Mono.defer {
            try {
                Mono.just(jwtParser.parseClaimsJws(token).body)
            } catch (e: UnsupportedJwtException) {
                Mono.empty()
            } catch (e: MalformedJwtException) {
                Mono.empty()
            } catch (e: SignatureException) {
                Mono.empty()
            } catch (e: ExpiredJwtException) {
                Mono.empty()
            } catch (e: IllegalArgumentException) {
                Mono.empty()
            }
        }
    }
}