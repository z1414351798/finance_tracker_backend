package com.z.finance.tracker.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SavingsGoal {
    private Long id;
    private Long userId;
    private String name;
    private String icon;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate deadline;
    private LocalDate createdAt;
}