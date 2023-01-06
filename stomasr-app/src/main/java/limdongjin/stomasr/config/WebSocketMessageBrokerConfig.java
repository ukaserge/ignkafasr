package limdongjin.stomasr.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan("limdongjin.stomasr.stomp")
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer  {
    @Value("${limdongjin.stomasr.cors.origin}")
    public String allowedOrigin;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // user endpoint
        registry.enableSimpleBroker("/topic", "/queue");

        // client -> server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(allowedOrigin)
                .withSockJS()
        ;
    }
}
