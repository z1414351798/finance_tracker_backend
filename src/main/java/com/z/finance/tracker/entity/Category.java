package com.z.finance.tracker.entity;

import com.z.finance.tracker.enums.TraType; // Ensure this is your INCOME/EXPENSE enum
import lombok.Data;

@Data
public class Category {
    private Long id;
    private String name;
    private TraType type; // Using the same Enum as Transactions
    private Long userId;
}