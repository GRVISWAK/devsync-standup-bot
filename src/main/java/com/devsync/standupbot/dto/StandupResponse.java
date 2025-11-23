package com.devsync.standupbot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for standup response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandupResponse {
    private Long standupId;
    private String userName;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate standupDate;
    
    private String yesterdayWork;
    private String todayPlan;
    private String blockers;
    private String aiSummary;
    private List<String> githubCommits;
    private List<String> jiraTasks;
    private List<String> calendarEvents;
    private String status;
    private Integer currentStep;
    private String nextQuestion;
}
