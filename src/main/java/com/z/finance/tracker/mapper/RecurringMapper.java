package com.z.finance.tracker.mapper;

import com.z.finance.tracker.entity.RecurringTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecurringMapper {
    void insert(RecurringTransaction r);
    void update(RecurringTransaction r);
    void deleteById(@Param("id") Long id, @Param("userId") Long userId);
    void updateNextDueDate(@Param("id") Long id, @Param("userId") Long userId,
                           @Param("nextDueDate") String nextDueDate);

    List<RecurringTransaction> findByUserId(@Param("userId") Long userId);

    /** Upcoming: due within the next N days (for home screen banner). */
    List<RecurringTransaction> findUpcoming(@Param("userId") Long userId,
                                            @Param("days")   int    days);

    /** Auto-detect: find transaction name/amount patterns that repeat regularly. */
    List<Map<String, Object>> detectPatterns(@Param("userId") Long userId);
}
