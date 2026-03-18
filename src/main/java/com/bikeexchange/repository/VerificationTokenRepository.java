package com.bikeexchange.repository;

import com.bikeexchange.model.User;
import com.bikeexchange.model.VerificationToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);
    Optional<VerificationToken> findByUserAndType(User user, VerificationToken.TokenType type);
    void deleteByUserAndType(User user, VerificationToken.TokenType type);

    @Modifying
    @Query("delete from VerificationToken vt where vt.id = :id")
    int deleteByIdSafe(@Param("id") Long id);
}
