package com.privacy.privacyplatform.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    int countByEmailAndIpAddressAndAttemptedAtAfter(
            String email,
            String ipAddress,
            LocalDateTime after
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    void deleteOldAttempts(LocalDateTime before);
}