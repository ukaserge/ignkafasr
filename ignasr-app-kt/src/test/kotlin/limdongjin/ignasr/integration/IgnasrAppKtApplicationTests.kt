package limdongjin.ignasr.integration

import limdongjin.ignasr.handler.SpeechUploadHandler
import limdongjin.ignasr.router.SpeechRouter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.PropertySource
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@ActiveProfiles("test")
@SpringBootTest(properties = arrayOf("limdongjin.ignasr.ignite.mode=mock"))
class IgnasrAppKtApplicationTests @Autowired constructor(
	val speechRouter: SpeechRouter,
	val speechUploadHandler: SpeechUploadHandler
){

	@Test
	fun contextLoads() {
		val webTestClient: WebTestClient = WebTestClient.bindToRouterFunction(speechRouter.speechRoute(speechUploadHandler))
			.configureClient()
			.build()

		val result = webTestClient.get().uri("/")
			.accept(MediaType.ALL)
			.exchange()
			.returnResult(ByteArray::class.java)

		val res = String(result.responseBody.single().block()!!)

		println(res)
	}

}
