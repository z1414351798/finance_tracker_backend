package com.z.finance.tracker.service;

import com.z.finance.tracker.entity.Budget;
import com.z.finance.tracker.mapper.BudgetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    @Autowired private BudgetMapper budgetMapper;

    @Cacheable(value = "budgets", key = "'month:' + #month + ':user:' + #userId")
    public List<Budget> getBudgets(Long userId, String month) {
        return budgetMapper.findByUserAndMonth(userId, month);
    }

    @Cacheable(value = "budgets", key = "'progress:' + #month + ':user:' + #userId")
    public List<Map<String, Object>> getBudgetProgress(Long userId, String month) {
        return budgetMapper.getBudgetProgress(userId, month);
    }
}