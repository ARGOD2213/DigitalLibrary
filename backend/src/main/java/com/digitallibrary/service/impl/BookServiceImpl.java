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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookServiceImpl implements BookService {

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
    public BookResponse addBook(BookRequest bookRequest) {
        if (bookRepository.existsByIsbn(bookRequest.getIsbn())) {
            throw new DuplicateResourceException("A book with this ISBN already exists");
        }

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
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<BookResponse> books = bookRepository.findAll(pageable).map(BookResponse::fromEntity);
        return PageResponse.fromPage(books);
    }

    @Override
    public PageResponse<BookResponse> searchBooks(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return getAllBooks(page, size, "title", "asc");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"));
        Page<BookResponse> books = bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword, pageable)
                .map(BookResponse::fromEntity);
        return PageResponse.fromPage(books);
    }

    @Override
    public BookResponse getBookById(Long id) {
        return BookResponse.fromEntity(findBookOrThrow(id));
    }

    @Override
    public BookResponse updateBook(Long id, BookRequest bookRequest) {
        Book existingBook = findBookOrThrow(id);
        bookRepository.findByIsbn(bookRequest.getIsbn())
                .filter(book -> !book.getId().equals(id))
                .ifPresent(book -> {
                    throw new DuplicateResourceException("A different book already uses this ISBN");
                });

        applyEditableFields(existingBook, bookRequest);

        return BookResponse.fromEntity(bookRepository.save(existingBook));
    }

    @Override
    public void deleteBook(Long id) {
        Book book = findBookOrThrow(id);
        bookRepository.delete(book);
    }

    @Override
    public BookResponse uploadPartnerContent(BookRequest bookRequest, MultipartFile file, String uploaderEmail) {
        if (bookRepository.existsByIsbn(bookRequest.getIsbn())) {
            throw new DuplicateResourceException("A content item with this ISBN/reference already exists");
        }

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
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private void applyEditableFields(Book book, BookRequest bookRequest) {
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setCategory(bookRequest.getCategory());
        book.setIsbn(bookRequest.getIsbn());
        book.setAvailableCopies(bookRequest.getAvailableCopies());
        book.setContentType(bookRequest.getContentType());
        book.setAccessType(bookRequest.getAccessType());
        book.setDescription(bookRequest.getDescription());
        book.setPreviewText(bookRequest.getPreviewText());
        book.setPublisher(bookRequest.getPublisher());
    }
}
