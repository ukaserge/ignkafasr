package limdongjin.ignasr

import limdongjin.ignasr.repository.IgniteRepositoryImpl
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.clients.producer.RecordMetadataTest
import org.apache.kafka.common.TopicPartition
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.VoidAnswer1
import org.springframework.core.ResolvableType
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.MultipartInserter
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderResult
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object MyTestUtil {
    private fun prepareMultipartBodyBuilder(
        fileFieldValue: ClassPathResource,
        nameFieldValue: String,
        labelFieldValue: String,
        userIdFieldValue: String,
    ): MultipartBodyBuilder {
        val multipartBodyBuilder = MultipartBodyBuilder()
        multipartBodyBuilder.part("file", fileFieldValue)
        multipartBodyBuilder.part("name", nameFieldValue)
        multipartBodyBuilder.part("label", labelFieldValue)
        multipartBodyBuilder.part("userId", userIdFieldValue)
        return multipartBodyBuilder
    }

    fun prepareMultipartInserter(
        fileFieldValue: ClassPathResource,
        reqIdFieldValue: String,
        labelFieldValue: String,
        userIdFieldValue: String,
    ): MultipartInserter {
        val multipartBodyBuilder =
            prepareMultipartBodyBuilder(fileFieldValue, reqIdFieldValue, labelFieldValue, userIdFieldValue)
        return BodyInserters.fromMultipartData(multipartBodyBuilder.build())
    }

    fun prepareServerRequest(
        fileFieldValue: ClassPathResource,
        nameFieldValue: String,
        labelFieldValue: String,
        userIdFieldValue: String,
    ): ServerRequest? {
        val multipartBodyBuilder =
            prepareMultipartBodyBuilder(fileFieldValue, nameFieldValue, labelFieldValue, userIdFieldValue)
        val outputMessage = MockClientHttpRequest(HttpMethod.POST, URI.create("/"))
        val writeMono = MultipartHttpMessageWriter()
            .write(Mono.just(multipartBodyBuilder.build()), ResolvableType.NONE, MediaType.MULTIPART_FORM_DATA, outputMessage, mapOf())
        return writeMono
            .then(Mono.just(1))
            .map { unused: Int? ->
                val request = MockServerHttpRequest
                    .method(HttpMethod.POST, "http://test.com")
                    .accept(MediaType.MULTIPART_FORM_DATA)
                    .contentType(outputMessage.headers.contentType!!)
                    .body(outputMessage.body)
                ServerRequest.create(MockServerWebExchange.from(request), emptyList())
            }
            .block(Duration.ofSeconds(5))
    }

    fun emptySenderResultVoid(): SenderResult<Void?> {
        return object : SenderResult<Void?> {
            override fun recordMetadata(): RecordMetadata {
                return RecordMetadata(TopicPartition("foo", 1), 1L, 1, 1L, 1, 1)
            }

            override fun exception(): Exception {
                return RuntimeException()
            }

            override fun correlationMetadata(): Void? {
                return null
            }
        }
    }

//    fun <K, V> stubbingIgniteRepository(
//        igniteRepositoryImpl: IgniteRepositoryImpl,
//        cacheName: String?,
//        stubGet: Boolean,
//        stubPut: Boolean,
//        stubSize: Boolean,
//        stubPutAsync: Boolean,
//        m: ConcurrentHashMap<K, V>?,
//    ): ConcurrentHashMap<K, V> {
//        val igniteUploadCacheMock: ConcurrentHashMap<K, V>
//        igniteUploadCacheMock = m ?: ConcurrentHashMap()
//        //        ConcurrentHashMap<K, V> igniteUploadCacheMock = new ConcurrentHashMap<>();
//        println(cacheName)
//        println(igniteUploadCacheMock.hashCode())
//        if (stubGet) {
//            Mockito.`when`(igniteRepositoryImpl.get<K, Any>(Mockito.eq(cacheName), Mockito.any()))
//                .thenAnswer { param: InvocationOnMock ->
//                    println("GET")
//                    println(igniteUploadCacheMock.hashCode())
//                    println(igniteUploadCacheMock.size)
//                    val key = param.getArgument<K>(1)
//                    igniteUploadCacheMock[key]
//                }
//        }
//        if (stubPut) {
//            Mockito.`when`(igniteRepositoryImpl.put(Mockito.eq(cacheName), Mockito.any<K>(), Mockito.any<V>()))
//                .thenAnswer { param: InvocationOnMock ->
//                    println("PUT")
//                    println(igniteUploadCacheMock.hashCode())
//                    println(igniteUploadCacheMock.size)
//                    val key = param.getArgument<K>(1)
//                    val value = param.getArgument<V>(2)
//                    igniteUploadCacheMock[key] = value
//                    key
//                }
//        }
//        if (stubSize) {
//            Mockito.`when`(igniteRepositoryImpl.size<Any, Any>(Mockito.eq(cacheName)))
//                .thenAnswer { param: InvocationOnMock? ->
//                    println("SIZE")
//                    println(igniteUploadCacheMock.hashCode())
//                    println(igniteUploadCacheMock.size)
//                    igniteUploadCacheMock.size
//                }
//        }
//        if (stubPutAsync) {
//            Mockito.`when`<Mono<Mono<K>>>(
//                igniteRepositoryImpl.putAsync(
//                    Mockito.eq(cacheName),
//                    Mockito.any<K>(),
//                    Mockito.any<V>()
//                )
//            )
//                .thenAnswer { param: InvocationOnMock ->
//                    println("PUT ASYNC")
//                    println(igniteUploadCacheMock.hashCode())
//                    println(igniteUploadCacheMock.size)
//                    val key = param.getArgument<K>(1)
//                    val value = param.getArgument<V>(2)
//                    val wrap = Mono.just(key)
//                        .doOnNext { kk: K ->
//                            println("doOnNext")
//                            println(igniteUploadCacheMock.hashCode())
//                            igniteUploadCacheMock[key] = value
//                            println(igniteUploadCacheMock.size)
//                        }
//                    Mono.just(wrap)
//                }
//        }
//        return igniteUploadCacheMock
//    }
}