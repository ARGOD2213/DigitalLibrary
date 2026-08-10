package com.digitallibrary.service.impl;

import com.digitallibrary.dto.BookRequest;
import com.digitallibrary.dto.BookResponse;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.entity.Book;
import com.digitallibrary.exception.DuplicateResourceException;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.BookRepository;
import com.digitallibrary.service.BookService;
import com.digitallibrary.storage.FileStorageService;
import com.digitallibrary.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;
    private final FileStorageService fileStorageService;

    public BookServiceImpl(BookRepository bookRepository, AppUserRepository appUserRepository,
                           FileStorageService fileStorageService) {
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public BookResponse addBook(BookRequest bookRequest) {
        checkDuplicateIsbn(bookRequest.getIsbn(), null);
        Book book = new Book(
                bookRequest.getTitle(),
                bookRequest.getAuthor(),
                bookRequest.getCategory(),
                bookRequest.getIsbn(),
                bookRequest.getAvailableCopies()
        );
        applyEditableFields(book, bookRequest);
        return BookResponse.fromEntity(bookRepository.save(book));
    }

    @Override
    public PageResponse<BookResponse> getAllBooks(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, resolveSortField(sortBy)));
        Page<BookResponse> books = bookRepository
                .findByDeletedAtIsNullAndPublishedTrue(pageable)
                .map(BookResponse::fromEntity);
        return PageResponse.fromPage(books);
    }

    @Override
    public PageResponse<BookResponse> searchBooks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"));
        if (keyword == null || keyword.isBlank()) {
            return getAllBooks(page, size, "title", "asc");
        }
        Page<BookResponse> books = bookRepository.searchBooks(keyword, pageable).map(BookResponse::fromEntity);
        return PageResponse.fromPage(books);
    }

    @Override
    public BookResponse getBookById(Long id) {
        Book book = findBookOrThrow(id);
        book.setViewCount(book.getViewCount() + 1);
        bookRepository.save(book);
        return BookResponse.fromEntity(book);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long id, BookRequest bookRequest) {
        Book existingBook = findBookOrThrow(id);
        checkDuplicateIsbn(bookRequest.getIsbn(), id);
        applyEditableFields(existingBook, bookRequest);
        return BookResponse.fromEntity(bookRepository.save(existingBook));
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book book = findBookOrThrow(id);
        // Soft delete
        book.setDeletedAt(LocalDateTime.now());
        book.setPublished(false);
        book.setStatus("DELETED");
        bookRepository.save(book);
        log.info("Book soft-deleted: id={} title={}", id, book.getTitle());
    }

    @Override
    @Transactional
    public BookResponse publishBook(Long id) {
        Book book = findBookOrThrow(id);
        book.setPublished(true);
        book.setStatus("PUBLISHED");
        return BookResponse.fromEntity(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookResponse unpublishBook(Long id) {
        Book book = findBookOrThrow(id);
        book.setPublished(false);
        book.setStatus("DRAFT");
        return BookResponse.fromEntity(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookResponse uploadPartnerContent(BookRequest bookRequest, MultipartFile file, String uploaderEmail) {
        checkDuplicateIsbn(bookRequest.getIsbn(), null);
        StoredFile storedFile = fileStorageService.store(file);
        Long uploaderId = appUserRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Uploader account not found"))
                .getId();

        Book book = new Book();
        applyEditableFields(book, bookRequest);
        book.setFileName(storedFile.getFileName());
        book.setFileUrl(storedFile.getFileUrl());
        book.setUploadedByUserId(uploaderId);
        return BookResponse.fromEntity(bookRepository.save(book));
    }

    private Book findBookOrThrow(Long id) {
        return bookRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private void checkDuplicateIsbn(String isbn, Long excludeId) {
        if (isbn == null || isbn.isBlank()) return;
        bookRepository.findByIsbn(isbn)
                .filter(book -> excludeId == null || !book.getId().equals(excludeId))
                .ifPresent(book -> {
                    throw new DuplicateResourceException("A book with ISBN '" + isbn + "' already exists");
                });
    }

    private void applyEditableFields(Book book, BookRequest req) {
        book.setTitle(req.getTitle());
        book.setSubtitle(req.getSubtitle());
        book.setAuthor(req.getAuthor());
        book.setCategory(req.getCategory());
        book.setIsbn(req.getIsbn());
        book.setAvailableCopies(req.getAvailableCopies());
        if (req.getPrice() != null) book.setPrice(req.getPrice());
        book.setFree(req.isFree());
        book.setPublished(req.isPublished());
        book.setStatus(req.getStatus() != null ? req.getStatus() : "PUBLISHED");
        book.setContentType(req.getContentType());
        book.setAccessType(req.getAccessType());
        book.setDescription(req.getDescription());
        book.setPreviewText(req.getPreviewText());
        book.setPublisher(req.getPublisher());
        book.setTags(req.getTags());
        if (req.getCoverImageUrl() != null) book.setCoverImageUrl(req.getCoverImageUrl());
    }

    private String resolveSortField(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "title") {
            case "author" -> "authorName";
            case "category" -> "categoryName";
            case "price" -> "price";
            case "rating" -> "averageRating";
            case "sales" -> "totalSales";
            case "views" -> "viewCount";
            case "createdat", "created" -> "createdAt";
            default -> "title";
        };
    }
}
