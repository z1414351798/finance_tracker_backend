package com.z.finance.tracker.entity;

import lombok.Data;

@Data
public class Budget {
    private Long id;
    private Long userId;
    private Long categoryId;
    private Double limitAmount;
    private String month; // "2025-07"
    private String categoryName; // joined field
}