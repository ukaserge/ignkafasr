package limdongjin.authserver.service

import limdongjin.authserver.dto.*
import limdongjin.authserver.entity.PersonRefresh
import limdongjin.authserver.exception.MissingFieldsException
import limdongjin.authserver.security.JwtAuthProvider
import limdongjin.authserver.security.JwtTokenGenerator
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*
import kotlin.jvm.Throws

@Service
class PersonService(
    val template: R2dbcEntityTemplate,
    val passwordEncoder: PasswordEncoder,
    val tokenGenerator: JwtTokenGenerator,
    val authProvider: JwtAuthProvider
) {
    @Throws(AuthenticationCredentialsNotFoundException::class)
    fun login(name: String, rawPassword: String): Mono<PersonLoginResponse> {
        return template
            .selectOne(Query.query(Criteria.where("name").isEqual(name)), PersonLogin::class.java)
            .flatMap {  encodedPerson ->
                when(passwordEncoder.matches(rawPassword, encodedPerson.password)){
                    true -> Mono.just(name)
                    false -> Mono.empty()
                }
            }
            .switchIfEmpty(Mono.error(AuthenticationCredentialsNotFoundException("incorrect credential")))
            .flatMap {
                val accessToken = tokenGenerator.generate(name, listOf(SimpleGrantedAuthority("USER")))
                val refreshToken = tokenGenerator.generate(name, listOf(SimpleGrantedAuthority("USER")), isRefresh = true)
                val refreshId = UUID.randomUUID().toString()

                template
                    .insert(PersonRefresh(id = refreshId, accessToken = accessToken, refreshToken = refreshToken))
            }
            .map {
                PersonLoginResponse(name, "login success", it.accessToken, it.id)
            }
    }

    @Throws(DuplicateKeyException::class)
    fun register(name: String, rawPassword: String, role: String): Mono<PersonRegisterResponse> {
        return template.insert(PersonRegister(name = name, password = passwordEncoder.encode(rawPassword), role = role))
            .flatMap { personRegister ->
                Mono.just(PersonRegisterResponse(name = personRegister.name, msg = "register success"))
            }
    }

    @Throws(AuthenticationCredentialsNotFoundException::class)
    fun refresh(accessToken: String, refreshToken: String): Mono<PersonRefreshResponse> {
        return template
            .selectOne(
                Query.query(
                    Criteria.where("access_token").isEqual(accessToken)
                        .and(Criteria.where("id").isEqual(refreshToken))
                ),
                PersonRefresh::class.java
            )
            .flatMap { personRefresh ->
                authProvider.extractAuthentication(personRefresh.refreshToken)
            }
            .switchIfEmpty(Mono.error(AuthenticationCredentialsNotFoundException("credential not found")))
            .flatMap { token ->
                val newAccessToken = tokenGenerator.generate(token.name, token.authorities)
                val newRefreshToken = tokenGenerator.generate(token.name, token.authorities, isRefresh = true)
                val newRefreshId = UUID.randomUUID().toString()

                template
                    .insert(PersonRefresh(id = newRefreshId, accessToken = newAccessToken, refreshToken = newRefreshToken))
                    .map { PersonRefreshResponse(name = token.name, accessToken = newAccessToken, refreshToken = newRefreshId, msg = "refresh success") }
            }

    }
}