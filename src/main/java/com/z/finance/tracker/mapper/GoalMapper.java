package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.SavingsGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GoalMapper {
    void insert(SavingsGoal goal);
    List<SavingsGoal> findByUserId(Long userId);
    SavingsGoal findById(Long id);
    void update(SavingsGoal goal);
    void addAmount(@Param("id") Long id, @Param("amount") Double amount);
    void deleteById(@Param("id") Long id, @Param("userId") Long userId);
    void deleteByUserId(@Param("userId") Long userId);
}