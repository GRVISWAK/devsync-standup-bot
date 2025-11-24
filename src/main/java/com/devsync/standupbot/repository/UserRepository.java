package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Primary lookup by Zoho User ID (auto-detected from webhook)
    Optional<User> findByZohoUserId(String zohoUserId);

    // Email-based lookup (legacy, for manual registration)
    Optional<User> findByEmail(String email);

    // Organization queries
    List<User> findByOrganizationId(Long organizationId);

    List<User> findByOrganizationIdAndRole(Long organizationId, UserRole role);

    // Team queries
    List<User> findByTeamId(Long teamId);

    List<User> findByTeamIdAndRole(Long teamId, UserRole role);

    // Existence checks
    boolean existsByEmail(String email);

    boolean existsByZohoUserId(String zohoUserId);
}
