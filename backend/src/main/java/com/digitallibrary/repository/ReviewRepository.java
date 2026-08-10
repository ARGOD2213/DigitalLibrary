package com.digitallibrary.repository;

import com.digitallibrary.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByBookIdAndModeratedTrue(Long bookId, Pageable pageable);
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId AND r.moderated = true")
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);

    Page<Review> findByUserId(Long userId, Pageable pageable);
}
