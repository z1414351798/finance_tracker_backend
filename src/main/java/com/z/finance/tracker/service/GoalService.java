package com.z.finance.tracker.service;

import com.z.finance.tracker.entity.SavingsGoal;
import com.z.finance.tracker.mapper.GoalMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalService {

    @Autowired private GoalMapper goalMapper;

    @Cacheable(value = "goals", key = "'user:' + #userId")
    public List<SavingsGoal> getGoalsByUserId(Long userId) {
        return goalMapper.findByUserId(userId);
    }
}