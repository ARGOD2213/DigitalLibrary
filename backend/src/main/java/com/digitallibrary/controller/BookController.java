package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.BookRequest;
import com.digitallibrary.dto.BookResponse;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> addBook(@Valid @RequestBody BookRequest bookRequest) {
        BookResponse savedBook = bookService.addBook(bookRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Book added successfully", savedBook));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success("Books loaded successfully",
                bookService.getAllBooks(page, size, sortBy, sortDirection)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Book loaded successfully", bookService.getBookById(id)));
    }

    @GetMapping("/{id}/access-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getPresignedAccessUrl(@PathVariable Long id) {
        String accessUrl = bookService.getPresignedAccessUrl(id);
        return ResponseEntity.ok(ApiResponse.success("Pre-signed URL generated successfully", accessUrl));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully",
                bookService.searchBooks(keyword, page, size)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest bookRequest) {
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully",
                bookService.updateBook(id, bookRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Book deleted successfully", null));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> publishBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Book published successfully",
                bookService.publishBook(id)));
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> unpublishBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Book unpublished / moved to draft",
                bookService.unpublishBook(id)));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> uploadContent(
            @RequestPart("metadata") @Valid BookRequest bookRequest,
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        BookResponse savedBook = bookService.uploadPartnerContent(bookRequest, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Content uploaded successfully", savedBook));
    }

    @PostMapping(value = "/upload/zip", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<ApiResponse<BookResponse>> uploadZipBundle(
            @RequestPart("file") MultipartFile zipFile,
            Principal principal) {
        BookResponse savedBook = bookService.uploadZipBundle(zipFile, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ZIP bundle uploaded and processed successfully", savedBook));
    }
}
