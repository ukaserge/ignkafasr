package limdongjin.ignasr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.Arrays;
@Configuration
@EnableWebFlux
public class WebfluxConfig implements WebFluxConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("https://kafasr.limdongjin.com", "https://limdongjin-kube.du.r.appspot.com")
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
                .allowCredentials(true).maxAge(3600)
        ;

        // Add more mappings...
    }
}
