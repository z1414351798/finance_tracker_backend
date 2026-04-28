package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Category;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.CategoryMapper;
import com.z.finance.tracker.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public CategoryController(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/getAllByUserId")
    public List<Category> getCategories() {
        return categoryMapper.findByUserId(getCurrentUserId());
    }

    // GET /api/categories?type=EXPENSE
    @GetMapping
    public List<Category> getCategories(@RequestParam TraType type) {
        return categoryMapper.findByUserIdAndType(getCurrentUserId(), type);
    }

    // POST /api/categories
    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        Long userId = getCurrentUserId();
        
        // Prevent duplicates
        if (categoryMapper.findByNameAndType(userId, category.getName(), category.getType()) != null) {
            return ResponseEntity.badRequest().body("Category already exists");
        }
        
        category.setUserId(userId);
        categoryMapper.insert(category);
        return ResponseEntity.ok(category);
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}