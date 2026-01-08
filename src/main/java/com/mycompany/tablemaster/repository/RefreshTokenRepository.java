package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.RefreshToken;
import com.mycompany.tablemaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 토큰으로 조회 (유효한 토큰만)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 토큰 조회
     */
    List<RefreshToken> findByUser(User user);

    /**
     * 사용자의 유효한 토큰 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 토큰 무효화
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(@Param("user") User user);

    /**
     * 사용자의 모든 토큰 삭제
     */
    void deleteByUser(User user);

    /**
     * 만료된 토큰 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
