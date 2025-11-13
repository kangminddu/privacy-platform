package com.privacy.privacyplatform.auth.repository;

import com.privacy.privacyplatform.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndVerificationCodeAndVerifiedFalse(
            String email,
            String verificationCode
    );

    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}