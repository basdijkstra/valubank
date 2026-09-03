package com.valubank.accounts.dto;

public class LoginResponse {

    private Long customerId;
    private String username;
    private String fullName;
    private boolean admin;

    public LoginResponse() {
    }

    public LoginResponse(Long customerId, String username, String fullName, boolean admin) {
        this.customerId = customerId;
        this.username = username;
        this.fullName = fullName;
        this.admin = admin;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
