package com.z.finance.tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CacheInvalidationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void evictUserCategories(Long userId) {
        evictByPattern("categories::*user:" + userId + "*");
    }

    public void evictUserGoals(Long userId) {
        evictByPattern("goals::*user:" + userId + "*");
    }

    public void evictUserBudgets(Long userId) {
        evictByPattern("budgets::*user:" + userId + "*");
    }

    public void evictUserSummary(Long userId) {
        evictByPattern("summary::*user:" + userId + "*");
        evictByPattern("trends::*user:" + userId + "*");
        evictByPattern("dailyTrends::*user:" + userId + "*");
    }

    // Call this when a transaction is added/updated
    // — invalidates everything that depends on transaction data
    public void evictAllUserCaches(Long userId) {
        evictUserCategories(userId);
        evictUserGoals(userId);
        evictUserBudgets(userId);
        evictUserSummary(userId);
    }

    private void evictByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}