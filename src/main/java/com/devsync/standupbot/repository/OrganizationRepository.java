package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Organization entity
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    Optional<Organization> findByCreatedByZohoId(String createdByZohoId);

    boolean existsByName(String name);
}
