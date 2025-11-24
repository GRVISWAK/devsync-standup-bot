package com.devsync.standupbot.scheduler;

import com.devsync.standupbot.service.ZohoCliqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automated standup reminders
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "standup.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class StandupReminderScheduler {

    private final ZohoCliqService zohoCliqService;

    /**
     * Send daily standup reminder
     * Runs Monday to Friday at 9:00 AM
     */
    @Scheduled(cron = "${standup.reminder.cron:0 0 9 * * MON-FRI}")
    public void sendDailyReminder() {
        log.info("Executing daily standup reminder task");
        
        try {
            zohoCliqService.sendStandupReminder();
            log.info("Daily standup reminder sent successfully");
        } catch (Exception e) {
            log.error("Error sending daily standup reminder: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Send end-of-day summary
     * Runs Monday to Friday at 5:00 PM
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void sendDailySummary() {
        log.info("Executing daily summary task");
        
        try {
            String summaryText = "📊 **Daily Summary**\n\n" +
                    "The team's standup updates for today have been collected.\n" +
                    "Great work everyone! 🎉";
            
            zohoCliqService.sendMessage(summaryText);
            log.info("Daily summary sent successfully");
        } catch (Exception e) {
            log.error("Error sending daily summary: {}", e.getMessage(), e);
        }
    }
}
