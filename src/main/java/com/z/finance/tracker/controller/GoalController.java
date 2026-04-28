package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.SavingsGoal;
import com.z.finance.tracker.mapper.GoalMapper;
import com.z.finance.tracker.mapper.UserMapper;
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

    @GetMapping
    public List<SavingsGoal> getGoals() {
        return goalMapper.findByUserId(getCurrentUserId());
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody SavingsGoal goal) {
        goal.setUserId(getCurrentUserId());
        goal.setCurrentAmount(0.0);
        goal.setCreatedAt(LocalDate.now());
        if (goal.getIcon() == null || goal.getIcon().isBlank()) {
            goal.setIcon("🎯");
        }
        goalMapper.insert(goal);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGoal(@PathVariable Long id, @RequestBody SavingsGoal goal) {
        goal.setId(id);
        goal.setUserId(getCurrentUserId());
        goalMapper.update(goal);
        return ResponseEntity.ok(goal);
    }

    // POST /api/goals/{id}/contribute  body: { "amount": 5000 }
    @PostMapping("/{id}/contribute")
    public ResponseEntity<?> contribute(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        Double amount = body.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }
        // Verify goal belongs to user
        SavingsGoal goal = goalMapper.findById(id);
        if (goal == null || !goal.getUserId().equals(getCurrentUserId())) {
            return ResponseEntity.status(403).build();
        }
        goalMapper.addAmount(id, amount);
        // Return updated goal
        return ResponseEntity.ok(goalMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        goalMapper.deleteById(id, getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}