package limdongjin.ignasr.util

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferLimitException
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.function.Function
import javax.naming.InsufficientResourcesException
import javax.naming.LimitExceededException

object MultiPartUtil {
    fun toFunctionThatFieldNameToBytesMono(request: ServerRequest): Function<String, Mono<ByteArray>> {
        val entryFlux = request.multipartData()
            .flatMapIterable<Map.Entry<String, List<Part?>>>(MultiValueMap<String, Part?>::entries)

        return Function { fieldName: String ->
            toBytesMono(
                entryFlux,
                fieldName
            )
        }
    }

    fun toEntryPartsFlux(request: ServerRequest): Flux<Map.Entry<String, List<Part>>> {
        return request
            .multipartData()
            .flatMapIterable { obj: MultiValueMap<String, Part> -> obj.entries } //                .publishOn(Schedulers.parallel())
    }

    fun toBytesMono(entryPartsFlux: Flux<Map.Entry<String, List<Part?>>>, fieldName: String): Mono<ByteArray> {
        return entryPartsFlux
            .filter { (key): Map.Entry<String, List<Part?>> -> key == fieldName }
            .single()
            .onErrorMap(NoSuchElementException::class.java) { obj: NoSuchElementException -> missingRequiredFields(obj) }
            .onErrorMap(
                DataBufferLimitException::class.java
            ) { obj: DataBufferLimitException -> exceedBufferSizeLimit(obj) }
            .flatMap { obj: Map.Entry<String, List<Part?>> -> toBytesMono(obj) }
            .subscribeOn(Schedulers.parallel()) //                .publishOn(Schedulers.boundedElastic())
    }

    fun toBytesMono(entry: Map.Entry<String, List<Part?>>): Mono<ByteArray> {
        val part = entry.value[0]
        return DataBufferUtils
            .join(part!!.content())
            .map { dataBuffer: DataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .subscribeOn(Schedulers.parallel())
    }

    fun missingRequiredFields(throwable: NoSuchElementException): Throwable {
        throwable.printStackTrace()
        return InsufficientResourcesException("required field(s) is missing ")
    }

    fun exceedBufferSizeLimit(throwable: DataBufferLimitException): Throwable {
        throwable.printStackTrace()
        return LimitExceededException("Part exceeded the disk usage limit ")
    }
}