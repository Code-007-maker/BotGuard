package com.example.botguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BotguardApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotguardApplication.class, args);
    }
}
