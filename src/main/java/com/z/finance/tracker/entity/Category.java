package com.z.finance.tracker.entity;

import com.z.finance.tracker.enums.TraType; // Ensure this is your INCOME/EXPENSE enum

public class Category {
    private Long id;
    private String name;
    private TraType type; // Using the same Enum as Transactions
    private Long userId;

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

    public TraType getType() {
        return type;
    }

    public void setType(TraType type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}