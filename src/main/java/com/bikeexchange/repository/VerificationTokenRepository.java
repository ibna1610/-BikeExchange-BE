package com.bikeexchange.repository;

import com.bikeexchange.model.User;
import com.bikeexchange.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);
    Optional<VerificationToken> findByUserAndType(User user, VerificationToken.TokenType type);
    void deleteByUserAndType(User user, VerificationToken.TokenType type);
}
