package limdongjin.stomasr.config;

import limdongjin.stomasr.handler.MySignalHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.websocket.servlet.TomcatWebSocketServletWebServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Value("${limdongjin.stomasr.cors.origin}")
    public String allowedOrigin;

    private final WebSocketHandler signalHandler;

    public WebSocketConfig(@Qualifier("MySignalHandler") WebSocketHandler signalHandler) {
        this.signalHandler = signalHandler;
    }

    @Bean
    public TomcatWebSocketServletWebServerCustomizer tomcatWebSocketServletWebServerCustomizer() {
        return new TomcatWebSocketServletWebServerCustomizer();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if(allowedOrigin != null && !allowedOrigin.equals("*")){
            registry
                .addHandler(signalHandler, "/signal")
                .setAllowedOrigins(allowedOrigin)
                .withSockJS()
            ;
        }else {
           registry
                .addHandler(signalHandler, "/signal")
                .setAllowedOriginPatterns("http://localhost:3000")
                .withSockJS()
            ;

        }
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);

        return container;
    }
}
