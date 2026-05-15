package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.Budget;
import com.z.finance.tracker.entity.Category;
import com.z.finance.tracker.enums.TraType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface BudgetMapper {
    void insert(Budget budget);
    void update(Budget budget);
    void deleteById(@Param("id") Long id, @Param("userId") Long userId);
    List<Budget> findByUserAndMonth(@Param("userId") Long userId, @Param("month") String month);
    List<Budget> findByUserId(@Param("userId") Long userId);
    List<Map<String, Object>> getBudgetProgress(@Param("userId") Long userId, @Param("month") String month);
    void deleteByUserId(@Param("userId") Long userId);
}