package com.z.finance.tracker.entity;

import com.z.finance.tracker.enums.TraType;
import lombok.Data;

@Data
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
}