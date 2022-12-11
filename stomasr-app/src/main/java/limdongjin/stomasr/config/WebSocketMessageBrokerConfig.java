package limdongjin.stomasr.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan("limdongjin.stomasr.stomp")
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer  {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // user endpoint
        registry.enableSimpleBroker("/topic", "/queue");

        // client -> server
        registry.setApplicationDestinationPrefixes("/app");

//        registry.setUserDestinationPrefix("/secured/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins("https://kafasr.limdongjin.com", "https://limdongjin-kube.du.r.appspot.com", "http://limdongjin-kube.du.r.appspot.com")
                .setAllowedOriginPatterns("https://kafasr.limdongjin.com",  "https://limdongjin-kube.du.r.appspot.com", "http://limdongjin-kube.du.r.appspot.com")
                .withSockJS()
        ;
    }

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration){
//        registration.interceptors(new ChannelInterceptor() {
//            @Override
//            public Message<?> preSend(Message<?> message, MessageChannel channel) {
//                StompHeaderAccessor accessor =
//                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//                    String user = accessor.getFirstNativeHeader("user");
//                    if (user != null) {
//                        List<GrantedAuthority> authorities = new ArrayList<>();
//                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
//                        Authentication auth = new UsernamePasswordAuthenticationToken(user, user, authorities);
//                        SecurityContextHolder.getContext().setAuthentication(auth);
//                        accessor.setUser(auth);
//                    }
//                }
//
//                return message;
//            }
//        });
//    }
}
