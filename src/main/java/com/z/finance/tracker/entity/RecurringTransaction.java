package com.z.finance.tracker.entity;

public class RecurringTransaction {
    private Long   id;
    private Long   userId;
    private String text;
    private Double amount;        // always stored positive
    private String type;          // EXPENSE | INCOME
    private Long   categoryId;
    private String categoryName;  // joined
    private String frequency;     // WEEKLY | BIWEEKLY | MONTHLY | QUARTERLY | YEARLY
    private String nextDueDate;   // yyyy-MM-dd
    private Boolean isActive;
    private String note;

    // ── getters / setters ────────────────────────────────────────────────────
    public Long    getId()           { return id; }
    public void    setId(Long v)     { this.id = v; }

    public Long    getUserId()           { return userId; }
    public void    setUserId(Long v)     { this.userId = v; }

    public String  getText()             { return text; }
    public void    setText(String v)     { this.text = v; }

    public Double  getAmount()           { return amount; }
    public void    setAmount(Double v)   { this.amount = v; }

    public String  getType()             { return type; }
    public void    setType(String v)     { this.type = v; }

    public Long    getCategoryId()       { return categoryId; }
    public void    setCategoryId(Long v) { this.categoryId = v; }

    public String  getCategoryName()         { return categoryName; }
    public void    setCategoryName(String v) { this.categoryName = v; }

    public String  getFrequency()            { return frequency; }
    public void    setFrequency(String v)    { this.frequency = v; }

    public String  getNextDueDate()          { return nextDueDate; }
    public void    setNextDueDate(String v)  { this.nextDueDate = v; }

    public Boolean getIsActive()             { return isActive; }
    public void    setIsActive(Boolean v)    { this.isActive = v; }

    public String  getNote()                 { return note; }
    public void    setNote(String v)         { this.note = v; }
}
