package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Category;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.CategoryMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.CacheInvalidationService;
import com.z.finance.tracker.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final CacheInvalidationService cacheInvalidation;

    public CategoryController(CategoryMapper categoryMapper,
                              UserMapper userMapper,
                              CategoryService categoryService,
                              CacheInvalidationService cacheInvalidation) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.categoryService = categoryService;
        this.cacheInvalidation = cacheInvalidation;
    }

    @GetMapping("/getAllByUserId")
    public List<Category> getAllCategories() {
        return categoryService.getAllByUserId(getCurrentUserId());
    }

    @GetMapping
    public List<Category> getCategories(@RequestParam TraType type) {
        return categoryService.getByUserIdAndType(getCurrentUserId(), type);
    }

    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        Long userId = getCurrentUserId();

        if (categoryMapper.findByNameAndType(
                userId, category.getName(), category.getType()) != null) {
            return ResponseEntity.badRequest().body("Category already exists");
        }

        category.setUserId(userId);
        categoryMapper.insert(category);
        cacheInvalidation.evictUserCategories(userId);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        categoryMapper.deleteById(id, userId);
        cacheInvalidation.evictUserCategories(userId);
        return ResponseEntity.ok("Deleted");
    }

    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username).getId();
    }
}