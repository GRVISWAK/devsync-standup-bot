package com.devsync.standupbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for DevSync Standup Bot
 * Integrates with Zoho Cliq for automated standup management
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableRetry
public class StandupBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandupBotApplication.class, args);
    }
}
