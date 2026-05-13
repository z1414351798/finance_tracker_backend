package com.z.finance.tracker.entity;

import com.z.finance.tracker.enums.TraType;

public class Transaction {
    private Long id;
    private String text;
    private Double amount;
    private Long categoryId;
    private String date;
    private Long userId;
    private TraType type;
    private String note;
    private String categoryName;
    private String imageUrl;        // ← add this
    private String imagePresignedUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TraType getType() {
        return type;
    }

    public void setType(TraType type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePresignedUrl() {
        return imagePresignedUrl;
    }

    public void setImagePresignedUrl(String imagePresignedUrl) {
        this.imagePresignedUrl = imagePresignedUrl;
    }
}