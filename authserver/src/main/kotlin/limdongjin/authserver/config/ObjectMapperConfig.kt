package limdongjin.authserver.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.ConstructorDetector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.Errors

@Configuration
class ObjectMapperConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        val kotlinModule = KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, true)
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
        val javaTimeModule = JavaTimeModule()

        return ObjectMapper()
            .registerModule(kotlinModule)
            .registerModule(javaTimeModule)
    }
}