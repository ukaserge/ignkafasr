package limdongjin.ignasr.handler

import limdongjin.ignasr.MyTestUtil.emptySenderResultVoid
import limdongjin.ignasr.MyTestUtil.prepareMultipartInserter
import limdongjin.ignasr.MyTestUtil.prepareServerRequest
import limdongjin.ignasr.repository.IgniteRepository
import limdongjin.ignasr.repository.MockIgniteRepository
import limdongjin.ignasr.router.SpeechRouter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.kafka.sender.SenderRecord
import java.io.IOException
import java.util.*

//@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SpeechUploadHandlerTest {
    lateinit var igniteRepository: IgniteRepository
    lateinit var reactiveKafkaProducerTemplate: ReactiveKafkaProducerTemplate<String?, ByteArray>
    lateinit var speechUploadHandler: SpeechUploadHandler

    @BeforeEach
    fun setUp() {
        reactiveKafkaProducerTemplate = mock()
        igniteRepository = MockIgniteRepository()
        speechUploadHandler = SpeechUploadHandler(igniteRepository, reactiveKafkaProducerTemplate)
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun uploadHandlerFunctionTestByInvokeDirectly() {
        // given file for testing
        val wavClassPathResource = ClassPathResource("data/foo.wav")
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val fileContent = wavClassPathResource.inputStream.readAllBytes()
        val label = "limdongjin"

        // Stubbing
        val cacheName = "uploadCache"
        whenever(reactiveKafkaProducerTemplate.send(any<SenderRecord<String?, ByteArray, Void>>()))
            .thenReturn(Mono.just(emptySenderResultVoid()))

        // Prepare ServerRequest
        val request = prepareServerRequest(wavClassPathResource, reqId, label, userId)
        println(request)

        // Directly Invoke Handler Function
        speechUploadHandler!!.upload(request!!).block()

        // Verify Behaviours
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(cacheName))
        Assertions.assertEquals(
            String(fileContent),
            String(igniteRepository!!.get<UUID, ByteArray>(cacheName, UUID.fromString(reqId)))
        )
        verify(reactiveKafkaProducerTemplate, times(1))
            .send(any<SenderRecord<String?, ByteArray, Any>>())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun registerHandlerFunctionTestByInvokeDirectly() {
        // given file for testing
        val wavClassPathResource = ClassPathResource("data/foo.wav")
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val fileContent = wavClassPathResource.inputStream.readAllBytes()
        val label = "limdongjin"

        // Prepare ServerRequest
        val request = prepareServerRequest(wavClassPathResource, reqId, label, userId)
        println(request)

        // Directly Invoke Handler Function
        speechUploadHandler!!.register(request!!).block()

        // Verify Behaviours
        val uploadCache = "uploadCache"
        val cacheName2 = "uuid2label"
        val cacheName3 = "authCache"
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(uploadCache))
        Assertions.assertEquals(
            String(fileContent),
            String(igniteRepository!!.get<UUID, ByteArray>(uploadCache, UUID.fromString(reqId)))
        )
        verify(reactiveKafkaProducerTemplate, times(0))
            .send(anyString(), any<ByteArray>())
        Thread.sleep(1000L)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun uploadHandlerFunctionTestByWebTestClient() {
        // Given file for testing
        val wavClassPathResource = ClassPathResource("data/foo.wav")
        val fileContent = wavClassPathResource.inputStream.readAllBytes()
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val label = "limdongjin"
        // Given cache names
        val uploadCache = "uploadCache"
        val reqId2userId = "reqId2userId"

        whenever(reactiveKafkaProducerTemplate.send(any<SenderRecord<String?, ByteArray, Void>>()))
            .thenReturn(Mono.just(emptySenderResultVoid()))

        // Prepare MultipartBody
        val multipartInserter = prepareMultipartInserter(wavClassPathResource, reqId, label, userId)

        // Execute Request and Verify Response
        val webTestClient = WebTestClient.bindToRouterFunction(SpeechRouter().speechRoute(speechUploadHandler!!))
            .configureClient()
            .build()
        val entityExchangeResult = webTestClient
            .post().uri("/api/speech/upload")
            .accept(MediaType.MULTIPART_FORM_DATA)
            .body(multipartInserter)
            .exchange() // Verify Response Format
            .expectBody()
            .jsonPath("$.userName").isEqualTo(reqId)
            .jsonPath("$.message").exists()
            .returnResult()
        println(String(entityExchangeResult.responseBodyContent!!))

        // must request contains file blob content
        Assertions.assertTrue(
            String(entityExchangeResult.requestBodyContent!!)
                .contains(String(fileContent))
        )

        // must send event to kafka topic
        verify(reactiveKafkaProducerTemplate, times(1))
            .send(any<SenderRecord<String?, ByteArray, Void>>())

        // must store blob to ignite
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(uploadCache))
        Assertions.assertEquals(
            String(fileContent),
            String(igniteRepository!!.get<UUID, ByteArray>(uploadCache, UUID.fromString(reqId)))
        )
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(reqId2userId))
        Assertions.assertEquals(
            UUID.fromString(userId),
            igniteRepository!!.get<UUID, UUID>(reqId2userId, UUID.fromString(reqId))
        )
        Assertions.assertEquals(0, igniteRepository!!.size<Any, Any>("authCache"))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun registerHandlerFunctionTestByWebTestClient() {
        // Given file for testing
        val wavClassPathResource = ClassPathResource("data/foo.wav")
        val fileContent = wavClassPathResource.inputStream.readAllBytes()
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val label = "ignite"
        // Given cache names
        val uploadCache = "uploadCache"
        val authCache = "authCache"
        val uuid2label = "uuid2label"

        // Prepare MultipartBody
        val multipartInserter = prepareMultipartInserter(wavClassPathResource, reqId, label, userId)

        // Execute Request and Verify Response
        val webTestClient = WebTestClient.bindToRouterFunction(SpeechRouter().speechRoute(speechUploadHandler!!))
            .configureClient()
            .build()
        val entityExchangeResult = webTestClient
            .post().uri("/api/speech/register")
            .accept(MediaType.MULTIPART_FORM_DATA)
            .body(multipartInserter)
            .exchange() // Verify Response Format
            .expectBody()
            .jsonPath("$.userName").isEqualTo(reqId)
            .jsonPath("$.message").exists()
            .returnResult()

        // must request contains file blob content
        Assertions.assertTrue(
            String(entityExchangeResult.requestBodyContent!!)
                .contains(String(fileContent))
        )

        // never send event to kafka topic
        verify(reactiveKafkaProducerTemplate, never()).send(
            eq("user-pending"), any<ByteArray>()
        )

        // must store blob to ignite cache
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(uploadCache))
        Assertions.assertEquals(
            String(fileContent),
            String(igniteRepository!!.get<UUID, ByteArray>(uploadCache, UUID.fromString(reqId)))
        )

        // must store registered uuid to ignite cache
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(authCache))
        Assertions.assertEquals(UUID.fromString(reqId), igniteRepository!!.get(authCache, UUID.fromString(reqId)))
        Assertions.assertEquals(1, igniteRepository!!.size<Any, Any>(uuid2label))
        Assertions.assertEquals(label, igniteRepository!!.get<UUID, String>(uuid2label, UUID.fromString(reqId)))
    }
}