package com.z.finance.tracker.dto;

import lombok.Data;

@Data
public class DailyTrendDTO {
    private String date;
    private Double income;
    private Double expense;

    // Getters and Setters
}