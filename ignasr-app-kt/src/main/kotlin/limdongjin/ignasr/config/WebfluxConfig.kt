package limdongjin.ignasr.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.util.Logger
import reactor.util.Loggers
import java.time.Duration

@Configuration
@EnableWebFlux
class WebfluxConfig(
    @Value("\${limdongjin.ignasr.cors.origin}")
    val allowedOrigin: String
): WebFluxConfigurer {
    companion object {
        val logger: Logger = Loggers.getLogger(WebFluxConfigurer::class.java)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        logger.info("CORS allowedOrigin = $allowedOrigin")

        if (allowedOrigin != "*") {
            registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
                .allowCredentials(true).maxAge(3600)
        } else {
            logger.info("DEVELOPMENT MODE. ALLOW LOCALHOST!!!")
            registry.addMapping("/api/**")
                .allowedOriginPatterns("http://*:3000", "http://localhost:*0")
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
        }
    }

    @Bean
    fun resourceFactory(): ReactorResourceFactory {
        val factory = ReactorResourceFactory()
        factory.isUseGlobalResources = false
        return factory
    }

    @Bean
    fun webClient(): WebClient {
        val httpClient = HttpClient.create(
            ConnectionProvider
                .builder("myConnectionProvider")
                // .maxConnections(8)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(20))
                // .lifo()
                .build()
        ).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 27000)
            .responseTimeout(Duration.ofSeconds(20))
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
