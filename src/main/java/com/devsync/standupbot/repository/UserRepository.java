package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByZohoUserId(String zohoUserId);

    boolean existsByEmail(String email);

    boolean existsByZohoUserId(String zohoUserId);
}
