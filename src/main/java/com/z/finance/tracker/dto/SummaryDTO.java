package com.z.finance.tracker.dto;

import java.util.List;
import java.util.Map;

public class SummaryDTO {
    private Double totalBalance;
    private Map<String, Object> monthlyBalance;
    private Map<String, Object> cashFlow;
    private List<Map<String, Object>> incomeCategories;
    private List<Map<String, Object>> expenseCategories;

    // Constructors
    public SummaryDTO() {}

    public SummaryDTO(Double totalBalance,
                      Map<String, Object> monthlyBalance,
                      Map<String, Object> cashFlow,
                      List<Map<String, Object>> incomeCategories,
                      List<Map<String, Object>> expenseCategories) {
        this.totalBalance = totalBalance;
        this.monthlyBalance = monthlyBalance;
        this.cashFlow = cashFlow;
        this.incomeCategories = incomeCategories;
        this.expenseCategories = expenseCategories;
    }

    // Getters and Setters
    public Double getTotalBalance() { return totalBalance; }
    public void setTotalBalance(Double totalBalance) { this.totalBalance = totalBalance; }

    public Map<String, Object> getMonthlyBalance() { return monthlyBalance; }
    public void setMonthlyBalance(Map<String, Object> monthlyBalance) { this.monthlyBalance = monthlyBalance; }

    public Map<String, Object> getCashFlow() { return cashFlow; }
    public void setCashFlow(Map<String, Object> cashFlow) { this.cashFlow = cashFlow; }

    public List<Map<String, Object>> getIncomeCategories() { return incomeCategories; }
    public void setIncomeCategories(List<Map<String, Object>> incomeCategories) { this.incomeCategories = incomeCategories; }

    public List<Map<String, Object>> getExpenseCategories() { return expenseCategories; }
    public void setExpenseCategories(List<Map<String, Object>> expenseCategories) { this.expenseCategories = expenseCategories; }
}