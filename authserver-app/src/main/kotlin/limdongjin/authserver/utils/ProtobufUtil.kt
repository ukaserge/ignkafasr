package limdongjin.authserver.utils

import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono

object ProtobufUtil {
    inline fun <reified T: GeneratedMessageV3> formRequestToProtobufObject(request: ServerRequest, instance: GeneratedMessageV3.Builder<*>): Mono<Any> {
        return request
            .formData()
            .map {
                it.toSingleValueMap()
            }
            .map { m ->
                val fields = instance.descriptorForType.fields
                var builder = instance

                fields.forEach { field ->
                    when (field.name) {
                        in m -> builder = builder.setField(field, m[field.name])
                        else -> builder = builder.clearField(field)
                    }
                }
                builder
            }
    }
}