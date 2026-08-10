package com.digitallibrary.audit;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.AuditLog;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    public AuditAspect(AuditLogRepository auditLogRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Audited audited, Object result) {
        saveAuditLog(audited.action(), audited.entity(), "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, Audited audited, Exception ex) {
        saveAuditLog(audited.action(), audited.entity(), "FAILURE", ex.getMessage());
    }

    private void saveAuditLog(String action, String entity, String status, String errorMessage) {
        try {
            String username = null;
            AppUser user = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
                Optional<AppUser> userOpt = appUserRepository.findByEmail(username);
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                }
            }

            String ipAddress = getClientIp();
            String details = errorMessage != null ? "Error: " + errorMessage : null;

            AuditLog auditLog = new AuditLog(user, username, action, entity, null, ipAddress, status, details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not extract IP address: {}", e.getMessage());
        }
        return "unknown";
    }
}
