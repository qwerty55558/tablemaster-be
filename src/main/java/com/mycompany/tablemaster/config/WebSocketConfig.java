package com.mycompany.tablemaster.config;

import com.mycompany.tablemaster.security.JwtTokenProvider;
import com.mycompany.tablemaster.service.TokenBlacklistService;
import com.mycompany.tablemaster.websocket.JwtChannelInterceptor;
import com.mycompany.tablemaster.websocket.WebSocketSessionHandlerDecorator;
import com.mycompany.tablemaster.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // 클라이언트로 메시지 전달 prefix
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{3000, 3000})  // 서버→클라 3초, 클라→서버 3초
                .setTaskScheduler(taskScheduler);

        // 클라이언트 → 서버 메시지 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 특정 사용자(디바이스)에게 전달 prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new JwtChannelInterceptor(jwtTokenProvider, tokenBlacklistService, sessionRegistry)
        );
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler ->
            new WebSocketSessionHandlerDecorator(handler, sessionRegistry)
        );
    }
}
