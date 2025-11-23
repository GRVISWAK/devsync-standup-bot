package com.devsync.standupbot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for standup request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandupRequest {
    private String zohoUserId;
    
    @NotBlank(message = "User email is required")
    @Email(message = "Invalid email format")
    private String userEmail;
    
    private String userName;
    
    @Size(max = 5000, message = "Response cannot exceed 5000 characters")
    private String response;
    
    private Integer step;
}
