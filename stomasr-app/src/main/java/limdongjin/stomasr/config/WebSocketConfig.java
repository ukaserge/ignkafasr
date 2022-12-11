package limdongjin.stomasr.config;

import limdongjin.stomasr.handler.MySignalHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler signalHandler;

    public WebSocketConfig(@Qualifier("MySignalHandler") WebSocketHandler signalHandler) {
        this.signalHandler = signalHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(signalHandler, "/signal")
//                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("https://kafasr.limdongjin.com", "https://limdongjin-kube.du.r.appspot.com", "http://limdongjin-kube.du.r.appspot.com")
                .withSockJS()
        ;
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);

        return container;
    }
}
