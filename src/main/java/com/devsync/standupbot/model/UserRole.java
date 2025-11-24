package com.devsync.standupbot.model;

/**
 * User roles for access control in organization hierarchy
 */
public enum UserRole {
    ORG_ADMIN,      // Organization admin - full control over org, all teams, and users
    TEAM_LEAD,      // Team leader - can manage their team, add users, view reports
    DEVELOPER       // Regular developer - can submit standups, view own history
}
