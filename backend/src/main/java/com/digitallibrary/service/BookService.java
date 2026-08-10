package com.digitallibrary.service;

import com.digitallibrary.dto.BookRequest;
import com.digitallibrary.dto.BookResponse;
import com.digitallibrary.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BookService {
    BookResponse addBook(BookRequest bookRequest);
    PageResponse<BookResponse> getAllBooks(int page, int size, String sortBy, String sortDirection);
    PageResponse<BookResponse> searchBooks(String keyword, int page, int size);
    BookResponse getBookById(Long id);
    BookResponse updateBook(Long id, BookRequest bookRequest);
    void deleteBook(Long id);
    BookResponse publishBook(Long id);
    BookResponse unpublishBook(Long id);
    BookResponse uploadPartnerContent(BookRequest bookRequest, MultipartFile file, String uploaderEmail);
    BookResponse uploadZipBundle(MultipartFile zipFile, String uploaderEmail);
    String getPresignedAccessUrl(Long bookId);
}
