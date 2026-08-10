package com.digitallibrary.dto;

import com.digitallibrary.entity.AuditLog;
import java.time.LocalDateTime;

public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entity;
    private String entityId;
    private String ipAddress;
    private String status;
    private String details;
    private LocalDateTime createdAt;

    public AuditLogResponse() {}

    public static AuditLogResponse fromEntity(AuditLog log) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(log.getId());
        if (log.getUser() != null) r.setUserId(log.getUser().getId());
        r.setUsername(log.getUsername());
        r.setAction(log.getAction());
        r.setEntity(log.getEntity());
        r.setEntityId(log.getEntityId());
        r.setIpAddress(log.getIpAddress());
        r.setStatus(log.getStatus());
        r.setDetails(log.getDetails());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
