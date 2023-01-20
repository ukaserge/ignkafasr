package limdongjin.stomasr.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

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
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(3184700);
        registry.setSendTimeLimit(20 * 10000);
        registry.setSendBufferSizeLimit(3 * 512 * 1024);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if(allowedOrigin != null && !allowedOrigin.equals("*")){
            registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(allowedOrigin)
                .withSockJS()
            ;
        }else {
            registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("http://localhost:3000")
                .withSockJS()
            ;

        }
    }
}
