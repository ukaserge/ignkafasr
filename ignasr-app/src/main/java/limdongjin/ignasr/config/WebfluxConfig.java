package limdongjin.ignasr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebfluxConfig implements WebFluxConfigurer {
    @Value("${limdongjin.ignasr.cors.origin}")
    public String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("CORS DEB");
        System.out.println(allowedOrigin);

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("PUT", "DELETE", "POST", "OPTIONS", "GET", "HEAD", "PUT")
                .allowCredentials(true).maxAge(3600)
        ;

        // Add more mappings...
    }
}
