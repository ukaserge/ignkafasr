package limdongjin.ignasr.handler

import limdongjin.ignasr.dto.SpeechUploadResponseDto
import limdongjin.ignasr.protos.UserPendingProto
import limdongjin.ignasr.repository.IgniteRepository
import limdongjin.ignasr.util.MultiPartUtil
import org.apache.kafka.common.utils.Time
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import reactor.util.Loggers
import reactor.util.function.Tuple2
import reactor.util.function.Tuple4
import java.util.*
import java.util.function.Function

@Component
class SpeechUploadHandler(
    private val igniteRepository: IgniteRepository,
    private val reactiveKafkaProducerTemplate: ReactiveKafkaProducerTemplate<String?, ByteArray>,
) {
    @Value("\${limdongjin.ignasr.cors.origin}")
    val allowedOrigin: String? = null

    fun index(request: ServerRequest?): Mono<ServerResponse> {
        return ServerResponse.ok().bodyValue("hello world")
    }

    fun upload(request: ServerRequest): Mono<ServerResponse> {
        val fieldNameToBytesMono: Function<String, Mono<ByteArray>> =
            MultiPartUtil.toFunctionThatFieldNameToBytesMono(request)

        val userIdMono = fieldNameToBytesMono.apply("userId")
            .map { s: ByteArray? -> UUID.fromString(String(s!!)) }

        val reqIdMono = fieldNameToBytesMono.apply("name")
            .map { s: ByteArray? -> UUID.fromString(String(s!!)) }

        val fileMono = fieldNameToBytesMono.apply("file")

        val fileUploadMonoMono: Mono<Mono<UUID>> = Mono.zip(reqIdMono, fileMono)
            .flatMap { reqId2file: Tuple2<UUID, ByteArray> ->
                igniteRepository.putAsync(
                    "uploadCache",
                    reqId2file.t1,
                    reqId2file.t2
                )
            }

        val userIdUploadMonoMono: Mono<Mono<UUID>> = Mono.zip(reqIdMono, userIdMono)
            .flatMap { reqId2userId: Tuple2<UUID, UUID> ->
                igniteRepository.putAsync(
                    "reqId2userId",
                    reqId2userId.t1,
                    reqId2userId.t2
                )
            }

        val kafkaProduceMono: Mono<Tuple2<UUID, UUID>> = Mono.zip(reqIdMono, userIdMono)
            .flatMap { tuple2: Tuple2<UUID, UUID> ->
                val userPendingBytes: ByteArray = UserPendingProto.UserPending
                    .newBuilder()
                    .setReqId(tuple2.t1.toString())
                    .setUserId(tuple2.t2.toString())
                    .build()
                    .toByteArray()
                reactiveKafkaProducerTemplate
                    .send(
                        SenderRecord.create<String?, ByteArray, Any?>(
                            "user-pending",
                            Random().nextInt(10),
                            Time.SYSTEM.milliseconds(),
                        null,
                            userPendingBytes,
                            null
                        )
                    )
                    .retry(2)
                    .flatMap { Mono.just<Tuple2<UUID, UUID>>(tuple2) }
            }
        return Mono.zip(fileUploadMonoMono, userIdUploadMonoMono)
            .flatMap { tuple2: Tuple2<Mono<UUID>, Mono<UUID>> ->
                Mono.zip(tuple2.t1, tuple2.t2)
                    .flatMap { _: Tuple2<UUID, UUID> -> kafkaProduceMono }
            }
            .flatMap { tuple2: Tuple2<UUID, UUID> ->
                toSuccessResponseDtoMono(
                    tuple2.t1,
                    "success upload; userId = ",
                    tuple2.t2.toString()
                )
            }
            .flatMap { responseDto: SpeechUploadResponseDto ->
                ServerResponse.ok()
                    .headers(::addCorsHeaders)
                    .body(Mono.just<Any>(responseDto), SpeechUploadResponseDto::class.java)
            }
    }

    fun register(request: ServerRequest): Mono<ServerResponse> {
        val fieldNameToBytesMono: Function<String, Mono<ByteArray>> =
            MultiPartUtil.toFunctionThatFieldNameToBytesMono(request)
        val uuidMono = fieldNameToBytesMono.apply("name")
            .map { s: ByteArray -> UUID.fromString(String(s)) }
        val fileMono = fieldNameToBytesMono.apply("file")
        val labelMono = fieldNameToBytesMono.apply("label")
            .map { bytes: ByteArray -> String(bytes) }

        val fileUploadMonoMono: Mono<Mono<UUID>> = Mono.zip(uuidMono, fileMono)
            .flatMap { uuid2file: Tuple2<UUID, ByteArray> ->
                igniteRepository.putAsync(
                    "uploadCache",
                    uuid2file.t1,
                    uuid2file.t2
                )
            }
        val labelUploadMonoMono: Mono<Mono<UUID>> = Mono.zip(labelMono, uuidMono)
            .flatMap { label2uuid: Tuple2<String, UUID> ->
                igniteRepository.putAsync(
                    "uuid2label",
                    label2uuid.t2,
                    label2uuid.t1
                )
            }
        val authUploadMonoMono: Mono<Mono<UUID>> = uuidMono
            .flatMap { uuid: UUID ->
                igniteRepository.putAsync(
                    "authCache",
                    uuid,
                    uuid
                )
            }
        return Mono.zip(authUploadMonoMono, fileUploadMonoMono, labelUploadMonoMono, Mono.just<Mono<String>>(labelMono))
            .flatMap<Any> { t: Tuple4<Mono<UUID>, Mono<UUID>, Mono<UUID>, Mono<String>> ->
                val authUploadMono = t.t1
                val fileUploadMono = t.t2
                val labelUploadMono = t.t3
                val labelMonoo = t.t4
                Mono.zip(authUploadMono, fileUploadMono, labelUploadMono, labelMonoo)
                    .flatMap { tuple3: Tuple4<UUID, UUID, UUID, String> ->
                        toSuccessResponseDtoMono(tuple3.t1, "success register; ", tuple3.t4)
                    }
            }
            .flatMap { responseDto: Any ->
                ServerResponse.ok()
                    .headers(::addCorsHeaders)
                    .body(Mono.just<Any>(responseDto), SpeechUploadResponseDto::class.java)
            }
    }

    private fun addCorsHeaders(httpHeaders: HttpHeaders) {
        if (allowedOrigin == "*") {
            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
        } else {
            httpHeaders.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin)
        }
        httpHeaders.addAll(
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            listOf("POST", "PUT", "OPTIONS", "GET", "HEAD")
        )
    }

    companion object {
//        private val logger = Loggers.getLogger(SpeechUploadHandler::class.java)
        fun toSuccessResponseDtoMono(reqId: UUID, msg: String, label: String): Mono<SpeechUploadResponseDto> {
            return Mono.just(
                SpeechUploadResponseDto(
                    reqId.toString(),
                    String.format("%s; %s; %s", msg, reqId.toString(), label),
                    label
                )
            )
        }
    }
}