package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.Organization;
import com.devsync.standupbot.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Team entity
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByTeamName(String teamName);

    Optional<Team> findByOrganizationAndTeamName(Organization organization, String teamName);

    List<Team> findByOrganization(Organization organization);

    Optional<Team> findByZohoChannelId(String zohoChannelId);

    boolean existsByTeamName(String teamName);
}
