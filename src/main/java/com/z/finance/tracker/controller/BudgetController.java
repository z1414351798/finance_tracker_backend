package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Budget;
import com.z.finance.tracker.mapper.BudgetMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.BudgetService;
import com.z.finance.tracker.service.CacheInvalidationService;
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
    private final BudgetService budgetService;
    private final CacheInvalidationService cacheInvalidation;

    public BudgetController(BudgetMapper budgetMapper,
                            UserMapper userMapper,
                            BudgetService budgetService,
                            CacheInvalidationService cacheInvalidation) {
        this.budgetMapper = budgetMapper;
        this.userMapper = userMapper;
        this.budgetService = budgetService;
        this.cacheInvalidation = cacheInvalidation;
    }

    @GetMapping
    public List<Budget> getBudgets(@RequestParam String month) {
        return budgetService.getBudgets(getCurrentUserId(), month);
    }

    @GetMapping("/progress")
    public ResponseEntity<List<Map<String, Object>>> getProgress(
            @RequestParam String month) {
        return ResponseEntity.ok(
                budgetService.getBudgetProgress(getCurrentUserId(), month)
        );
    }

    @PostMapping
    public ResponseEntity<?> createBudget(@RequestBody Budget budget) {
        Long userId = getCurrentUserId();
        budget.setUserId(userId);
        budgetMapper.insert(budget);
        cacheInvalidation.evictUserBudgets(userId);
        return ResponseEntity.ok(budget);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBudget(
            @PathVariable Long id,
            @RequestBody Budget budget) {
        Long userId = getCurrentUserId();
        budget.setId(id);
        budget.setUserId(userId);
        budgetMapper.update(budget);
        cacheInvalidation.evictUserBudgets(userId);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        budgetMapper.deleteById(id, userId);
        cacheInvalidation.evictUserBudgets(userId);
        return ResponseEntity.ok("Deleted");
    }

    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}