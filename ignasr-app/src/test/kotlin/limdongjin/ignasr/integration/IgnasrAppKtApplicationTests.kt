package limdongjin.ignasr.integration

import limdongjin.ignasr.security.JwtAuthProvider
import limdongjin.ignasr.security.JwtTokenGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*
import java.util.stream.Collectors

@ActiveProfiles("test")
@SpringBootTest(properties = arrayOf("limdongjin.ignasr.ignite.mode=mock"))
class IgnasrAppKtApplicationTests @Autowired constructor(
	val applicationContext: ApplicationContext
){
	@Test
	fun contextLoads() {
		val webTestClient: WebTestClient = WebTestClient
//			.bindToRouterFunction(speechRouter.speechRoute(speechUploadHandler))
			.bindToApplicationContext(applicationContext)
			.configureClient()
			.build()

		val result = webTestClient.get().uri("/api/user/foo")
			.accept(MediaType.ALL)
			.exchange()
			.returnResult(ByteArray::class.java)
		Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.status)
		println(result.status)

		val result2 = webTestClient.get().uri("/")
			.accept(MediaType.ALL)
			.exchange()
			.returnResult(ByteArray::class.java)
		Assertions.assertEquals(HttpStatus.OK, result2.status)
		println(result2.status)

		val keyFileContent = applicationContext.environment.getProperty("limdongjin.ignasr.security.token")
		println(keyFileContent)
		val authorities = listOf(SimpleGrantedAuthority("USER"))

		val token = JwtTokenGenerator(keyFileContent!!)
			.signedBuilder()
			.setSubject("DJ")
			.setIssuer("ignasr")
			.setExpiration(Date(Date().time + 30_000))
			.claim(
				"authorities",
				authorities.parallelStream()
					.map { (it as GrantedAuthority).authority }
					.collect(Collectors.joining(","))
			)
			.compact()

		val result3 = webTestClient.get().uri("/api/user/foo")
			.accept(MediaType.ALL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
			.exchange()
			.returnResult(ByteArray::class.java)
		println(result3.status)
		println(String(result3.responseBody.blockFirst()!!))
	}

}
