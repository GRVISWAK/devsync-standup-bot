package com.devsync.standupbot.model;

/**
 * User roles for access control
 */
public enum UserRole {
    ADMIN,      // Can configure team settings
    MANAGER,    // Can view all team standups
    MEMBER      // Regular team member
}
