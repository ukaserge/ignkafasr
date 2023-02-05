package limdongjin.authserver.config

import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.core.DatabaseClient

@Configuration
@EnableR2dbcRepositories
class R2dbcConfig(
    @Value("\${spring.r2dbc.host}")
    val HOST: String,

    @Value("\${spring.r2dbc.protocol}")
    val PROTOCOL: String,

    @Value("\${spring.r2dbc.database}")
    val DATABASE: String,

    @Value("\${spring.r2dbc.port}")
    val PORT: String,

    @Value("\${spring.r2dbc.username}")
    val USER: String,

    @Value("\${spring.r2dbc.password}")
    val PASSWORD: String
){
    @Bean
    fun r2dbcEntityTemplate(connectionFactory: ConnectionFactory): R2dbcEntityTemplate {
        return R2dbcEntityTemplate(connectionFactory)
    }

    @Bean
    fun connectionFactory(): ConnectionFactory {
        return ConnectionFactoryBuilder.withOptions(ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, HOST)
            .option(ConnectionFactoryOptions.PROTOCOL, PROTOCOL)
            .option(ConnectionFactoryOptions.DATABASE, DATABASE)
            .option(ConnectionFactoryOptions.PORT, PORT.toInt())
            .option(ConnectionFactoryOptions.USER, USER)
            .option(ConnectionFactoryOptions.PASSWORD, PASSWORD)
        ).build()
    }

    @Bean
    fun r2dbcClient(connectionFactory: ConnectionFactory): DatabaseClient {
        return DatabaseClient.builder().connectionFactory(connectionFactory).build()
    }
}