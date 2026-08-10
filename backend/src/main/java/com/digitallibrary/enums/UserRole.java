package com.digitallibrary.enums;

public enum UserRole {
    ROLE_ADMIN,
    ROLE_VENDOR,
    ROLE_USER;

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) return ROLE_USER;
        String normalized = roleStr.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        if ("ROLE_PARTNER".equals(normalized)) {
            return ROLE_VENDOR;
        }
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return ROLE_USER;
        }
    }
}
