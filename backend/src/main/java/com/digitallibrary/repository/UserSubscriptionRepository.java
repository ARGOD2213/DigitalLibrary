package com.digitallibrary.repository;

import com.digitallibrary.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate < :now")
    List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);
    
    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);
}
