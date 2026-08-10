package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.AuditLogResponse;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.entity.AuditLog;
import com.digitallibrary.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved",
                PageResponse.fromPage(logs.map(AuditLogResponse::fromEntity))));
    }

    @GetMapping("/by-user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByUser(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findByUsernameOrderByCreatedAtDesc(
                username, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Audit logs for user retrieved",
                PageResponse.fromPage(logs.map(AuditLogResponse::fromEntity))));
    }

    @GetMapping("/by-action")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByAction(
            @RequestParam String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findByActionOrderByCreatedAtDesc(
                action, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success("Audit logs for action retrieved",
                PageResponse.fromPage(logs.map(AuditLogResponse::fromEntity))));
    }
}
