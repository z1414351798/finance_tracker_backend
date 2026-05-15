package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    void insert(User user);
    User findByUsername(String username);
    User findById(Long id);
    void updateProfile(User user);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
    User findByEmail(String email);
    void deleteById(@Param("id") Long id);
}