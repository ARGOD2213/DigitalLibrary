package com.digitallibrary.repository;

import com.digitallibrary.entity.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Page<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId, Pageable pageable);
    Optional<ReadingHistory> findByUserIdAndBookId(Long userId, Long bookId);

    // Fetch category names from books the user has read — for recommendations
    @Query("SELECT DISTINCT b.categoryName FROM ReadingHistory rh JOIN rh.book b WHERE rh.user.id = :userId AND b.categoryName IS NOT NULL")
    List<String> findReadCategoriesByUserId(@Param("userId") Long userId);
}
