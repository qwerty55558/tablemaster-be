package com.mycompany.tablemaster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    /**
     * Redis Keyspace Notification을 수신하기 위한 리스너 컨테이너
     * - __keyevent@*__:expired 패턴으로 key 만료 이벤트 구독
     * - Redis 설정에서 notify-keyspace-events Ex 활성화 필요
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
