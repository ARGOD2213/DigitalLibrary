package com.digitallibrary.dto;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.enums.SubscriptionPlan;
import com.digitallibrary.enums.UserRole;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private SubscriptionPlan subscriptionPlan;
    private String organizationName;

    public static UserResponse fromEntity(AppUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setSubscriptionPlan(user.getSubscriptionPlan());
        response.setOrganizationName(user.getOrganizationName());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
}
