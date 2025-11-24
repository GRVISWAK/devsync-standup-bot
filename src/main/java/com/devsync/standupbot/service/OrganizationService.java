package com.devsync.standupbot.service;

import com.devsync.standupbot.model.Organization;
import com.devsync.standupbot.model.User;
import com.devsync.standupbot.model.UserRole;
import com.devsync.standupbot.repository.OrganizationRepository;
import com.devsync.standupbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing organizations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    
    /**
     * Register new organization
     * The creator becomes ORG_ADMIN automatically
     */
    @Transactional
    public Organization registerOrganization(String orgName, String domain, String creatorZohoId, String creatorName, String creatorEmail) {
        // Check if organization already exists
        if (organizationRepository.existsByName(orgName)) {
            throw new IllegalArgumentException("Organization '" + orgName + "' already exists");
        }
        
        // Check if user already belongs to an organization
        Optional<User> existingUser = userRepository.findByZohoUserId(creatorZohoId);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("You already belong to organization: " + existingUser.get().getOrganization().getName());
        }
        
        // Create organization
        Organization organization = Organization.builder()
            .name(orgName)
            .domain(domain)
            .createdByZohoId(creatorZohoId)
            .createdByName(creatorName)
            .active(true)
            .build();
        
        organization = organizationRepository.save(organization);
        log.info("Organization created: {} by {}", orgName, creatorName);
        
        // Create user as ORG_ADMIN
        User admin = User.builder()
            .organization(organization)
            .zohoUserId(creatorZohoId)
            .name(creatorName)
            .email(creatorEmail)
            .role(UserRole.ORG_ADMIN)
            .build();
        
        userRepository.save(admin);
        log.info("User {} registered as ORG_ADMIN for organization {}", creatorName, orgName);
        
        return organization;
    }
    
    /**
     * Get organization by name
     */
    public Optional<Organization> getOrganizationByName(String name) {
        return organizationRepository.findByName(name);
    }
    
    /**
     * Get organization by creator Zoho ID
     */
    public Optional<Organization> getOrganizationByCreator(String zohoUserId) {
        return organizationRepository.findByCreatedByZohoId(zohoUserId);
    }
    
    /**
     * Check if organization exists
     */
    public boolean organizationExists(String name) {
        return organizationRepository.existsByName(name);
    }
}
