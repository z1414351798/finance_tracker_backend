package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.UserConsent;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ConsentMapper {
    void insert(UserConsent consent);
    List<UserConsent> findByUserId(Long userId);
}
