package com.example.botguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Lua script for atomic horizontal capping (Check-Then-Act).
     * 
     * KEYS[1] = The Redis key for the post's bot reply counter (e.g., "post:123:bot_replies")
     * ARGV[1] = The maximum allowed limit (e.g., 100)
     * 
     * Returns:
     * 1 if increment was successful (under cap)
     * 0 if the cap has been reached
     */
    @Bean
    public RedisScript<Long> horizontalCapScript() {
        String script = """
            local current = redis.call('get', KEYS[1])
            if current and tonumber(current) >= tonumber(ARGV[1]) then
                return 0
            end
            redis.call('incr', KEYS[1])
            return 1
            """;
        return RedisScript.of(script, Long.class);
    }
}
