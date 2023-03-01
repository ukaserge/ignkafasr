package limdongjin.stomasr.config;

import org.springframework.boot.autoconfigure.websocket.servlet.TomcatWebSocketServletWebServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@ComponentScan("limdongjin.stomasr.stomp")
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfig implements WebSocketMessageBrokerConfigurer  {
    @Value("${limdongjin.stomasr.cors.origin}")
    public String allowedOrigin;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // user endpoint
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler())
                .setHeartbeatValue(new long[]{10000L, 10000L})
        ;

        // client -> server
        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    private TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        return scheduler;
    }
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(3184700);
        registry.setSendTimeLimit(20 * 10000);
        registry.setSendBufferSizeLimit(512 * 1024);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if(allowedOrigin != null && !allowedOrigin.equals("*")){
            registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(allowedOrigin)
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .withSockJS()
            ;
        }else {
            registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("http://localhost:3000")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .withSockJS()
            ;

        }
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
      registration.interceptors(new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
              System.out.println("preSend");
              System.out.println(message);
              System.out.println(channel);
              StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
              if(StompCommand.CONNECT.equals(accessor.getCommand())){
                  String user = accessor.getFirstNativeHeader("user");
                  if(user == null){
                      return message;
                  }
                  List<GrantedAuthority> authorities = new ArrayList<>();
                  authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                  UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user, user, authorities);
                  SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                  accessor.setUser(usernamePasswordAuthenticationToken);
              }
              return message;
          }
      });
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);

        return container;
    }
    @Bean
    public TomcatWebSocketServletWebServerCustomizer tomcatWebSocketServletWebServerCustomizer() {
        return new TomcatWebSocketServletWebServerCustomizer();
    }
}
