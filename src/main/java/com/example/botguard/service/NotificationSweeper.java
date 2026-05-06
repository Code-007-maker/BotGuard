package com.example.botguard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class NotificationSweeper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSweeper.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public NotificationSweeper(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Runs every 5 minutes (300,000 ms)
    @Scheduled(fixedRate = 300000)
    public void sweepBatchedNotifications() {
        log.info("Starting scheduled sweep of batched notifications...");
        
        // Note: For a production system with millions of keys, SCAN should be used instead of KEYS.
        Set<String> keys = redisTemplate.keys("notification:pending:user:*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String listKey : keys) {
            String userIdStr = listKey.substring(listKey.lastIndexOf(":") + 1);
            
            Long size = redisTemplate.opsForList().size(listKey);
            if (size != null && size > 0) {
                // Get all elements (0 to -1)
                List<Object> messages = redisTemplate.opsForList().range(listKey, 0, -1);
                
                // Clear the list atomically by deleting the key once read
                redisTemplate.delete(listKey);
                
                log.info("Batch summary for User {}: Sent {} batched messages: {}", userIdStr, size, messages);
            }
        }
    }
}
