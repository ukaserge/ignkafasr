package limdongjin.ignasr.handler

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import limdongjin.ignasr.MyTestUtil.prepareMultipartInserter
import limdongjin.ignasr.router.SpeechRouter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.function.Consumer

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@ExtendWith(
    SpringExtension::class
)
class SpeechUploadHandlerIntegrationTest {
    @Autowired
    private val speechUploadHandler: SpeechUploadHandler? = null

    @Autowired
    private val kafkaAdmin: KafkaAdmin? = null

    @Test
    fun is200OK() {
        kafkaAdmin!!.createOrModifyTopics(TopicBuilder.name("user-pending").partitions(10).build())
        val reqId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val label = "dong"
        val file = ClassPathResource("data/foo.wav")
        val multipartInserter = prepareMultipartInserter(file, reqId, label, userId)
        val webTestClient = WebTestClient.bindToRouterFunction(SpeechRouter().speechRoute(speechUploadHandler!!))
            .configureClient()
            .build()

        // Execute Request and Verify Response
        val ret1 = webTestClient
            .post().uri("/api/speech/upload")
            .accept(MediaType.MULTIPART_FORM_DATA)
            .body(multipartInserter)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult(String::class.java)
        ret1.responseBody.single()
            .subscribe { b: String ->
                println(b)
                Assertions.assertTrue(b.contains("success"))
            }
        val ret2 = webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult(String::class.java)
        println(ret2)
    }

    companion object {
        // container port binding
        // https://github.com/testcontainers/testcontainers-java/issues/256#issuecomment-405879835
        var hostPort = 20800
        var containerExposedPort = 10800
        var cmd: Consumer<CreateContainerCmd> =
            Consumer { e: CreateContainerCmd ->
                e.withPortBindings(
                    PortBinding(
                        Ports.Binding.bindPort(hostPort),
                        ExposedPort(containerExposedPort)
                    )
                )
            }

        @JvmStatic
        @Container
        var ignite: GenericContainer<*> = GenericContainer<Nothing>(DockerImageName.parse("apacheignite/ignite:2.14.0")).apply {
            withExposedPorts(containerExposedPort)
            withCreateContainerCmdModifier(cmd)
        }
            

        @JvmStatic
        @Container
        var kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))

        @JvmStatic
        @DynamicPropertySource
        fun kafkaProperties(registry: DynamicPropertyRegistry) {
            registry.add(
                "limdongjin.ignasr.kafka.bootstrapservers"
            ) { kafkaContainer.bootstrapServers }
            registry.add(
                "limdongjin.ignasr.ignite.addresses"
            ) {
                "localhost:" + ignite.getMappedPort(
                    10800
                ).toString()
            }
        }
    }
}