package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.SavingsGoal;
import com.z.finance.tracker.mapper.GoalMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.CacheInvalidationService;
import com.z.finance.tracker.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired private GoalMapper goalMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private GoalService goalService;
    @Autowired private CacheInvalidationService cacheInvalidation;

    @GetMapping
    public List<SavingsGoal> getGoals() {
        return goalService.getGoalsByUserId(getCurrentUserId());
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody SavingsGoal goal) {
        Long userId = getCurrentUserId();
        goal.setUserId(userId);
        goal.setCurrentAmount(0.0);
        goal.setCreatedAt(LocalDate.now());
        if (goal.getIcon() == null || goal.getIcon().isBlank()) goal.setIcon("🎯");
        goalMapper.insert(goal);
        cacheInvalidation.evictUserGoals(userId);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGoal(
            @PathVariable Long id,
            @RequestBody SavingsGoal goal) {
        Long userId = getCurrentUserId();
        goal.setId(id);
        goal.setUserId(userId);
        goalMapper.update(goal);
        cacheInvalidation.evictUserGoals(userId);
        return ResponseEntity.ok(goal);
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<?> contribute(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        Double amount = body.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        Long userId = getCurrentUserId();
        SavingsGoal goal = goalMapper.findById(id);
        if (goal == null || !goal.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        goalMapper.addAmount(id, amount);
        cacheInvalidation.evictUserGoals(userId);
        return ResponseEntity.ok(goalMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        goalMapper.deleteById(id, userId);
        cacheInvalidation.evictUserGoals(userId);
        return ResponseEntity.ok().build();
    }

    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}