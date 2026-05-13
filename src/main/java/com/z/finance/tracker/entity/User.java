package com.z.finance.tracker.entity;


public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String profileImageUrl;
    private String presignedImageUrl;

    public String getPresignedImageUrl() {
        return presignedImageUrl;
    }

    public void setPresignedImageUrl(String presignedImageUrl) {
        this.presignedImageUrl = presignedImageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}