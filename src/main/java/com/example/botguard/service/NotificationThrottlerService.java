package com.example.botguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NotificationThrottlerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationThrottlerService.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public NotificationThrottlerService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void pushNotification(Long userId, String message) {
        String throttleKey = "notification:throttle:user:" + userId;
        String listKey = "notification:pending:user:" + userId;
        
        // Phase 3: Atomic SET NX EX for the throttle window (15 minutes)
        Boolean canSendNow = redisTemplate.opsForValue().setIfAbsent(throttleKey, "1", Duration.ofMinutes(15));
        
        if (Boolean.TRUE.equals(canSendNow)) {
            // No recent notification sent, we can "send" instantly.
            log.info("Sending instant notification to User {}: {}", userId, message);
        } else {
            // Throttled! Push to the Redis List for batching.
            // Right Push (RPUSH) appends to the end of the list.
            redisTemplate.opsForList().rightPush(listKey, message);
            log.info("Throttled notification for User {}. Added to batch queue.", userId);
        }
    }
}
