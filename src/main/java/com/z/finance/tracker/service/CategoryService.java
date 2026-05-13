package com.z.finance.tracker.service;

import com.z.finance.tracker.entity.Category;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired private CategoryMapper categoryMapper;

    @Cacheable(value = "categories", key = "'all:user:' + #userId")
    public List<Category> getAllByUserId(Long userId) {
        return categoryMapper.findByUserId(userId);
    }

    @Cacheable(value = "categories", key = "'type:' + #type + ':user:' + #userId")
    public List<Category> getByUserIdAndType(Long userId, TraType type) {
        return categoryMapper.findByUserIdAndType(userId, type);
    }
}