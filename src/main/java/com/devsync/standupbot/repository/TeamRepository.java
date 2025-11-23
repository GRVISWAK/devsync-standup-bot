package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Team entity
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByTeamName(String teamName);

    Optional<Team> findByZohoChannelId(String zohoChannelId);

    boolean existsByTeamName(String teamName);
}
