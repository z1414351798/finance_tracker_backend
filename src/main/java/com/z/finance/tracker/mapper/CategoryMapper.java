package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.Category;
import com.z.finance.tracker.enums.TraType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CategoryMapper {
    // Fetch categories based on Type (Income vs Expense) for a specific user
    List<Category> findByUserIdAndType(@Param("userId") Long userId, @Param("type") TraType type);
    
    // Save a new custom category
    void insert(Category category);
    
    // Optional: Check if category already exists to avoid duplicates
    Category findByNameAndType(@Param("userId") Long userId, @Param("name") String name, @Param("type") TraType type);
    
    void deleteById(@Param("id") Long id, @Param("userId") Long userId);

    List<Category> findByUserId(@Param("userId") Long userId);
    void deleteByUserId(@Param("userId") Long userId);
}