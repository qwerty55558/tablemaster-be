package com.mycompany.tablemaster.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    private final StringRedisTemplate redisTemplate;

    /**
     * 앱 시작 시 Redis keyspace notification 활성화
     */
    @PostConstruct
    public void enableKeyspaceNotifications() {
        try {
            redisTemplate.getConnectionFactory().getConnection()
                    .serverCommands().setConfig("notify-keyspace-events", "Ex");
            log.info("Redis keyspace notifications enabled (notify-keyspace-events=Ex)");
        } catch (Exception e) {
            log.warn("Failed to set Redis keyspace notifications: {}", e.getMessage());
        }
    }

    /**
     * Redis Keyspace Notification을 수신하기 위한 리스너 컨테이너
     * - __keyevent@*__:expired 패턴으로 key 만료 이벤트 구독
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisKeyExpirationListener expirationListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // key 만료 이벤트 구독 (모든 DB)
        container.addMessageListener(expirationListener, new PatternTopic("__keyevent@*__:expired"));

        return container;
    }
}
