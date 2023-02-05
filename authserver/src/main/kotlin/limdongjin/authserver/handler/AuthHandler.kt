package limdongjin.authserver.handler

import com.fasterxml.jackson.databind.ObjectMapper
import limdongjin.authserver.dto.PersonLogin
import limdongjin.authserver.dto.PersonRegister
import limdongjin.authserver.dto.RefreshRequest
import limdongjin.authserver.exception.MissingFieldsException
import limdongjin.authserver.service.PersonService
import limdongjin.authserver.utils.ServerRequestUtil
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono

@Component
class AuthHandler(val objectMapper: ObjectMapper, val personService: PersonService) {
    fun register(request: ServerRequest): Mono<ServerResponse> {
        return ServerRequestUtil
            .dataFromFormRequest<PersonRegister>(request, objectMapper)
            .flatMap {
                personService.register(name = it.name, rawPassword = it.password, role = it.role)
            }
            .flatMap(ok()::bodyValue)
            .onErrorResume(MissingFieldsException::class.java) {
                badRequest().bodyValue(object {
                    val error = "required field(s) are missing.."
                    val msg = "register fail"
                })
            }
            .onErrorResume(DuplicateKeyException::class.java) {
                status(HttpStatus.CONFLICT).bodyValue(object {
                    val error = "duplicated name.."
                    val msg = "register fail"
                })
            }
    }

    fun login(request: ServerRequest): Mono<ServerResponse> {
        return ServerRequestUtil
            .dataFromFormRequest<PersonLogin>(request, objectMapper)
            .flatMap {
                personService.login(it.name, it.password)
            }
            .flatMap(ok()::bodyValue)
            .onErrorResume(AuthenticationCredentialsNotFoundException::class.java) {
                status(HttpStatus.UNAUTHORIZED).bodyValue(object {
                    val error = "login fail"
                    val msg = "incorrect username or password"
                })
            }
            .onErrorResume(MissingFieldsException::class.java) {
                badRequest().bodyValue(object {
                    val error = "login fail"
                    val msg = "required field(s) are missing.."
                })
            }
    }

    fun refresh(request: ServerRequest): Mono<ServerResponse> {
        return ServerRequestUtil
            .dataFromFormRequest<RefreshRequest>(request, objectMapper)
            .flatMap {
                personService.refresh(it.accessToken, it.refreshToken)
            }
            .flatMap(ok()::bodyValue)
            .onErrorResume(CredentialsExpiredException::class.java) {
                status(HttpStatus.UNAUTHORIZED).bodyValue(object {
                    val error = "refresh fail."
                    val msg = "credential not found"
                })
            }
    }
}