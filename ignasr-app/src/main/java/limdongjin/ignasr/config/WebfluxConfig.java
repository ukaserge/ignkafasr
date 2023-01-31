package limdongjin.ignasr.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorNetty2ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.internal.shaded.reactor.pool.AllocationStrategy;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.function.Function;

@Configuration
@EnableWebFlux
public class WebfluxConfig implements WebFluxConfigurer {
    @Value("${limdongjin.ignasr.cors.origin}")
    public String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("CORS DEB");
        System.out.println(allowedOrigin);

        if(!allowedOrigin.equals("*")){
            registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
                .allowCredentials(true).maxAge(3600)
            ;
        }else {
            registry.addMapping("/api/**")
                .allowedOriginPatterns("http://*:3000", "http://localhost:*0")
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
            ;
        }
    }
    @Bean
    public ReactorResourceFactory resourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);
        return factory;
    }

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create(ConnectionProvider
                .builder("myConnectionProvider")
                .maxConnections(8)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(20))
                .lifo()
                .build()).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 27000)
                .responseTimeout(Duration.ofSeconds(20))

        ;

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build()
        ;
    }
}
