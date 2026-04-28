package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Budget;
import com.z.finance.tracker.mapper.BudgetMapper;
import com.z.finance.tracker.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetMapper budgetMapper;
    private final UserMapper userMapper;

    public BudgetController(BudgetMapper budgetMapper, UserMapper userMapper) {
        this.budgetMapper = budgetMapper;
        this.userMapper = userMapper;
    }

    // GET /api/budgets/progress?month=2025-07
    @GetMapping("/progress")
    public ResponseEntity<List<Map<String, Object>>> getProgress(
            @RequestParam String month) {
        return ResponseEntity.ok(budgetMapper.getBudgetProgress(getCurrentUserId(), month));
    }

    // GET /api/budgets?month=2025-07
    @GetMapping
    public List<Budget> getBudgets(@RequestParam String month) {
        return budgetMapper.findByUserAndMonth(getCurrentUserId(), month);
    }

    @PostMapping
    public ResponseEntity<?> createBudget(@RequestBody Budget budget) {
        budget.setUserId(getCurrentUserId());
        budgetMapper.insert(budget);
        return ResponseEntity.ok(budget);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        budget.setId(id);
        budget.setUserId(getCurrentUserId());
        budgetMapper.update(budget);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        budgetMapper.deleteById(id, getCurrentUserId());
        return ResponseEntity.ok("Deleted");
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}