package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserIntegration entity
 */
@Repository
public interface UserIntegrationRepository extends JpaRepository<UserIntegration, Long> {

    Optional<UserIntegration> findByUser(User user);

    Optional<UserIntegration> findByUserAndTeam(User user, com.devsync.standupbot.model.Team team);
}
