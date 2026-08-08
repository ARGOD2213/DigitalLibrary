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

    // POST is used when the client asks the server to create a new resource.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public ResponseEntity<ApiResponse<BookResponse>> addBook(@Valid @RequestBody BookRequest bookRequest) {
        BookResponse savedBook = bookService.addBook(bookRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Book added successfully", savedBook));
    }

    // Pagination keeps large tables fast later when the project grows.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        PageResponse<BookResponse> books = bookService.getAllBooks(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success("Books loaded successfully", books));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        BookResponse book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success("Book loaded successfully", book));
    }

    // Search books by title or author using the keyword query parameter.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        PageResponse<BookResponse> books = bookService.searchBooks(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", books));
    }

    // PUT replaces the editable fields of an existing book.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest bookRequest) {
        BookResponse updatedBook = bookService.updateBook(id, bookRequest);
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", updatedBook));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Book deleted successfully", null));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER')")
    public ResponseEntity<ApiResponse<BookResponse>> uploadContent(
            @RequestPart("metadata") @Valid BookRequest bookRequest,
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        BookResponse savedBook = bookService.uploadPartnerContent(bookRequest, file, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Content uploaded successfully", savedBook));
    }
}
