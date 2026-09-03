package com.valubank.accounts.dto;

public class LoginResponse {

    private Long customerId;
    private String username;
    private String fullName;

    public LoginResponse() {
    }

    public LoginResponse(Long customerId, String username, String fullName) {
        this.customerId = customerId;
        this.username = username;
        this.fullName = fullName;
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
}
