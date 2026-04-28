package com.z.finance.tracker.controller;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.TransactionMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    public TransactionController(TransactionMapper transactionMapper, UserMapper userMapper) {
        this.transactionMapper = transactionMapper;
        this.userMapper = userMapper;
    }

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrendDTO>> getDailyTrends(
            @RequestParam(defaultValue = "30") int days) {

        // Use the ID from your authenticated user
        List<DailyTrendDTO> trends = transactionService.getDailyTrendsForUser(
                getCurrentUserId(),
                days
        );

        return ResponseEntity.ok(trends);
    }

    @PostMapping("/add")
    public Transaction create(@RequestBody Transaction transaction) {
        transaction.setUserId(getCurrentUserId());
        transactionMapper.insert(transaction);
        return transaction;
    }

    /**
     * Advanced History Search
     * Supports: Pagination, Text/Note search, Category, Type, Amount Range, and Date Range.
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TraType type,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String note
    ) {

        Long userId = getCurrentUserId();
        int offset = page * size;

        // Pass all filter parameters to the mapper
        List<Transaction> list = transactionMapper.findAllFiltered(
                userId, offset, size, text, categoryId, type, minAmount, maxAmount, startDate, endDate, note
        );

        // Ensure count uses the same filters so pagination numbers are accurate
        long total = transactionMapper.countFiltered(
                userId, text, categoryId, type, minAmount, maxAmount, startDate, endDate, note
        );

        Map<String, Object> response = new HashMap<>();
        response.put("content", list);
        response.put("totalElements", total);
        return response;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(
            @RequestParam(defaultValue = "") String month) {
        Long userId = getCurrentUserId();
        // Default to current month if not provided
        String targetMonth = month.isEmpty()
                ? new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date())
                : month;

        Map<String, Object> res = new HashMap<>();
        res.put("totalBalance", transactionMapper.getTotalBalance(userId));
        res.put("monthlyBalance", transactionMapper.getMonthlyBalance(userId, targetMonth));
        res.put("cashFlow", transactionMapper.getIncomeVsExpense(userId));
        res.put("incomeCategories", transactionMapper.getCategorySummary(userId, TraType.INCOME));
        res.put("expenseCategories", transactionMapper.getCategorySummary(userId, TraType.EXPENSE));
        return res;
    }

    @GetMapping("/trends")
    public List<Map<String, Object>> getTrends(
            @RequestParam String range, // DAY, MONTH, YEAR
            @RequestParam TraType type) {
        return transactionMapper.getTimeSeriesData(getCurrentUserId(), range, type);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Transaction transaction) {
        // Validate that the transaction belongs to the current user
        Long currentUserId = getCurrentUserId();

        // In your Mapper, add an update method
        transaction.setId(id);
        transaction.setUserId(currentUserId);
        transactionMapper.update(transaction);

        return ResponseEntity.ok("Updated successfully");
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // It is safer to handle null checks here if your security config allows anonymous access
        var user = userMapper.findByUsername(username);
        return (user != null) ? user.getId() : null;
    }
}