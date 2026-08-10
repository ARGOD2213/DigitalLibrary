package com.digitallibrary.repository;

import com.digitallibrary.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Query("SELECT o FROM Otp o WHERE o.target = :target AND o.type = :type AND o.used = false " +
           "AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<Otp> findValidOtps(@Param("target") String target,
                             @Param("type") String type,
                             @Param("now") LocalDateTime now);

    Optional<Otp> findTopByTargetAndTypeAndUsedFalseOrderByCreatedAtDesc(String target, String type);

    @Query("SELECT COUNT(o) FROM Otp o WHERE o.target = :target AND o.type = :type AND o.createdAt >= :since")
    long countRecentOtps(@Param("target") String target,
                          @Param("type") String type,
                          @Param("since") LocalDateTime since);
}
