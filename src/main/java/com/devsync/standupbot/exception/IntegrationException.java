package com.devsync.standupbot.exception;

/**
 * Exception thrown when external integration fails
 */
public class IntegrationException extends RuntimeException {
    
    private final String integrationName;
    private final boolean retryable;
    
    public IntegrationException(String integrationName, String message) {
        super(String.format("%s integration error: %s", integrationName, message));
        this.integrationName = integrationName;
        this.retryable = true;
    }
    
    public IntegrationException(String integrationName, String message, Throwable cause) {
        super(String.format("%s integration error: %s", integrationName, message), cause);
        this.integrationName = integrationName;
        this.retryable = true;
    }
    
    public IntegrationException(String integrationName, String message, boolean retryable) {
        super(String.format("%s integration error: %s", integrationName, message));
        this.integrationName = integrationName;
        this.retryable = retryable;
    }
    
    public String getIntegrationName() {
        return integrationName;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}
