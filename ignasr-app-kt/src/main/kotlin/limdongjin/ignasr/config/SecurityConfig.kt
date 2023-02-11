package limdongjin.ignasr.config

import limdongjin.ignasr.security.filter.JwtAuthenticationFilter
import limdongjin.ignasr.security.JwtAuthProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    @Value("\${limdongjin.ignasr.security.token}")
    private val keyFileContent: String
){
    @Bean
    @DependsOn("methodSecurityExpressionHandler")
    fun apiHttpSecurity(http: ServerHttpSecurity,
                        reactiveUserDetailsService: ReactiveUserDetailsService
    ): SecurityWebFilterChain {
        return http
            .exceptionHandling {
                it.authenticationEntryPoint { exchange, ex ->
                    Mono.fromRunnable {
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    }
                }.accessDeniedHandler { exchange, denied ->
                    Mono.fromRunnable {
                        exchange.response.statusCode = HttpStatus.FORBIDDEN
                    }
                }
            }
            .csrf().disable()
            .cors().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exc ->
                exc.pathMatchers("/api/user/**").hasAuthority("USER")
                    .pathMatchers(*WHITELIST_URL).permitAll()
                    .anyExchange().permitAll()
            }
            .addFilterAt(JwtAuthenticationFilter(jwtProvider()), SecurityWebFiltersOrder.HTTP_BASIC)
            .build()
    }

    @Bean
    fun reactiveUserDetailsService(): ReactiveUserDetailsService {
        val user = User.withUsername("user").password("{noop}password").roles("USER").build()
        return MapReactiveUserDetailsService(user)
    }

    fun jwtProvider(): JwtAuthProvider {
        return JwtAuthProvider(keyFileContent)
    }

    companion object {
        val WHITELIST_URL = arrayOf(
            "/api/speech/upload",
            "/api/speech/register",
            "/api/speech/register2",
            "/api/speech/analysis",
            "/"
        )
    }

}
