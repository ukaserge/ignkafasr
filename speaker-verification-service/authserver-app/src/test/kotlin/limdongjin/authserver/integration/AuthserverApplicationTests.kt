package limdongjin.authserver.integration

import limdongjin.authserver.dto.PersonLoginResponse
import limdongjin.authserver.dto.PersonRegisterResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class AuthserverApplicationTests (
    @Autowired
    val applicationContext: ApplicationContext,

    @Autowired
    val template: R2dbcEntityTemplate
){
    lateinit var webTestClient: WebTestClient

    companion object {
        @Container
        @JvmStatic
        val postgresqlContainer = PostgreSQLContainer("postgres")
            .withUsername("dj")
            .withDatabaseName("djdb")
            .withPassword("1q2w3e4r")

        @JvmStatic
        @DynamicPropertySource
        fun connectionProperties(registry: DynamicPropertyRegistry){
            registry.add("spring.r2dbc.port") { postgresqlContainer.getMappedPort(5432) }
        }
    }

    @BeforeEach
    fun setUp() {
        template.databaseClient.sql("CREATE TABLE IF NOT EXISTS person (name VARCHAR(255) PRIMARY KEY, password VARCHAR(255), role VARCHAR(255))")
            .fetch()
            .rowsUpdated()
            .subscribe()

        template.databaseClient.sql("CREATE TABLE IF NOT EXISTS person_refresh (id VARCHAR(255) PRIMARY KEY , access_token VARCHAR(700), refresh_token VARCHAR(700))")
            .fetch()
            .rowsUpdated()
            .subscribe()
        webTestClient = WebTestClient
            .bindToApplicationContext(applicationContext)
            .configureClient()
            .build()
    }

    @Test
    fun indexTest(){
        val result = webTestClient.get()
            .uri("/")
            .exchange()
            .returnResult(ByteArray::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.status)
        println(String(result.responseBody.blockFirst()!!))
    }

    @Test
    fun canRegister() {
        val result = webTestClient.post()
            .uri("/api/register")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(validRegisterBody())
            .exchange()
            .returnResult(PersonRegisterResponse::class.java)
        println(result.status)
        println(result.responseBody.blockFirst()!!)

        // Verify Status Code
        Assertions.assertEquals(HttpStatus.OK, result.status)
    }

    @Test
    fun handleDuplicatedRegister() {
        val body = validRegisterBody()
        val result = webTestClient.post()
            .uri("/api/register")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .exchange()
            .returnResult(ByteArray::class.java)
        println(result.status)
        println(String(result.responseBody.blockFirst()!!))

        // Verify Status Code
        Assertions.assertEquals(HttpStatus.OK, result.status)

        val result2 = webTestClient.post()
            .uri("/api/register")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body.with("password", "other password"))
            .exchange()
            .returnResult(ByteArray::class.java)
        println(result2.status)
        println(String(result2.responseBody.blockFirst()!!))

        // Verify Status Code
        Assertions.assertEquals(HttpStatus.CONFLICT, result2.status)
    }

    @Test
    fun canLoginIfRegistered(){
        val username = UUID.randomUUID().toString()
        val password = UUID.randomUUID().toString()

        val registerResponse = webTestClient.post()
            .uri("/api/register")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(validRegisterBody(username, password))
            .exchange()
            .returnResult(PersonRegisterResponse::class.java)

        Assertions.assertEquals(HttpStatus.OK, registerResponse.status)

        val loginResponse = webTestClient.post()
            .uri("/api/login")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(validLoginBody(username, password))
            .exchange()
            .returnResult(PersonLoginResponse::class.java)

        Assertions.assertEquals(HttpStatus.OK, loginResponse.status)
        val res = loginResponse.responseBody.blockFirst()!!
        println(res)

        Assertions.assertNotNull(res.accessToken)
        Assertions.assertNotNull(res.refreshToken)
        Assertions.assertTrue {
            res.accessToken.isNotBlank() && res.refreshToken.isNotBlank() &&
                    res.accessToken.startsWith("ey")
        }
    }

    @Test
    fun canRefresh(){
        val username = UUID.randomUUID().toString()
        val password = UUID.randomUUID().toString()

        val registerResponse = webTestClient.post()
            .uri("/api/register")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(validRegisterBody(username, password))
            .exchange()
            .returnResult(PersonRegisterResponse::class.java)

        Assertions.assertEquals(HttpStatus.OK, registerResponse.status)

        val loginResponse = webTestClient.post()
            .uri("/api/login")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(validLoginBody(username, password))
            .exchange()
            .returnResult(PersonLoginResponse::class.java)

        Assertions.assertEquals(HttpStatus.OK, loginResponse.status)
        val res = loginResponse.responseBody.blockFirst()!!

        val refreshForm = LinkedMultiValueMap<String, String>()
        refreshForm.set("name", username)
        refreshForm.set("accessToken", res.accessToken)
        refreshForm.set("refreshToken", res.refreshToken)

        val result4 = webTestClient.post()
            .uri("/api/refresh")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(refreshForm))
            .exchange()
            .returnResult(ByteArray::class.java)
        println(result4.status)
        println(String(result4.responseBody.blockFirst()!!))

        Assertions.assertEquals(HttpStatus.OK, result4.status)
        val resRefresh = String(result4.responseBody.blockFirst()!!)
        println(resRefresh)
    }

    @Test
    fun handleIncorrectLoginRequest(){
        val incorrectLoginForm = LinkedMultiValueMap<String, String>()
        incorrectLoginForm.set("name", "invalid user")
        incorrectLoginForm.set("password", "invalid password !!!!")

        val result3 = webTestClient.post()
            .uri("/api/login")
            .accept(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(incorrectLoginForm))
            .exchange()
            .returnResult(ByteArray::class.java)
        println(result3.status)
        println(String(result3.responseBody.blockFirst()!!))

        // Verify Status Code
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result3.status)
    }

    private fun validRegisterBody(username: String = UUID.randomUUID().toString(), password: String = UUID.randomUUID().toString()): BodyInserters.FormInserter<String> {
        val validRegisterForm = LinkedMultiValueMap<String, String>()
        validRegisterForm.set("name", username)
        validRegisterForm.set("password", password)
        validRegisterForm.set("role", "USER")

        return BodyInserters.fromFormData(validRegisterForm)
    }

    private fun validLoginBody(username: String, password: String): BodyInserters.FormInserter<String> {
        val form = LinkedMultiValueMap<String, String>()
        form.set("name", username)
        form.set("password", password)

        return BodyInserters.fromFormData(form)
    }
}
