package com.digitallibrary.repository;

import com.digitallibrary.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Active (non-deleted) published books
    Page<Book> findByDeletedAtIsNullAndPublishedTrue(Pageable pageable);

    // Active book by id
    Optional<Book> findByIdAndDeletedAtIsNull(Long id);

    // Backward-compatible search delegate
    default Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author, Pageable pageable) {
        return searchBooks(title, pageable);
    }

    // Full text search across title, author, category, isbn - only non-deleted
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(b.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(b.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(b.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    // Popular books by total sales
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL AND b.published = true ORDER BY b.totalSales DESC")
    Page<Book> findPopularBooks(Pageable pageable);

    // Latest books
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL AND b.published = true ORDER BY b.createdAt DESC")
    Page<Book> findLatestBooks(Pageable pageable);

    // Free books
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL AND b.published = true AND b.free = true")
    Page<Book> findFreeBooks(Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);
}
