package limdongjin.authserver.utils

import com.fasterxml.jackson.databind.ObjectMapper
import limdongjin.authserver.exception.MissingFieldsException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import kotlin.jvm.Throws

object ServerRequestUtil {
    @Throws(MissingFieldsException::class)
    inline fun <reified T> dataFromFormRequest(request: ServerRequest, objectMapper: ObjectMapper): Mono<T> {
        return request
            .formData()
            .map { it.toSingleValueMap() }
            .map { objectMapper.convertValue(it, T::class.java) }
            .onErrorMap(IllegalArgumentException::class.java) {
                MissingFieldsException()
            }
    }
}