package com.devsync.standupbot.repository;

import com.devsync.standupbot.model.Standup;
import com.devsync.standupbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Standup entity
 */
@Repository
public interface StandupRepository extends JpaRepository<Standup, Long> {

    @Query("SELECT s FROM Standup s JOIN FETCH s.user WHERE s.id = :id")
    Optional<Standup> findById(@Param("id") Long id);

    @Query("SELECT s FROM Standup s JOIN FETCH s.user WHERE s.user = :user AND s.standupDate = :standupDate")
    Optional<Standup> findByUserAndStandupDate(@Param("user") User user, @Param("standupDate") LocalDate standupDate);

    List<Standup> findByUserAndStandupDateBetween(User user, LocalDate startDate, LocalDate endDate);

    List<Standup> findByStandupDateAndStatus(LocalDate standupDate, Standup.StandupStatus status);

    @Query("SELECT s FROM Standup s WHERE s.standupDate = :date AND s.status = 'COMPLETED'")
    List<Standup> findCompletedStandupsByDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM Standup s JOIN FETCH s.user WHERE s.user = :user ORDER BY s.standupDate DESC")
    List<Standup> findRecentStandupsByUser(@Param("user") User user);

    long countByUserAndStatus(User user, Standup.StandupStatus status);
}
