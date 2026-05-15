package com.z.finance.tracker.mapper;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.enums.TraType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionMapper {
    void insert(Transaction transaction);

    // TransactionMapper.java
    List<Transaction> findAllFiltered(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("text") String text,
            @Param("categoryId") Long categoryId,
            @Param("type") TraType type,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("note") String note
    );

    long countFiltered(
            @Param("userId") Long userId,
            @Param("text") String text,
            @Param("categoryId") Long categoryId,
            @Param("type") TraType type,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("note") String note
    );

    // Summary data for charts
    Map<String, Object> getIncomeVsExpense(@Param("userId") Long userId);

    List<Map<String, Object>> getCategorySummary(@Param("userId") Long userId, @Param("type") TraType type);


    List<Map<String, Object>> getTimeSeriesData(@Param("userId") Long userId, @Param("range") String range, @Param("type") TraType type);

    void update(Transaction transaction);

    List<DailyTrendDTO> getDailyTrends(@Param("userId") Long userId, @Param("days") int days);

    Map<String, Object> getMonthlyBalance(
            @Param("userId") Long userId,
            @Param("month") String month
    );
    Double getTotalBalance(@Param("userId") Long userId);

    // Add these two method signatures:
    List<Map<String, Object>> getMonthlyBreakdown(@Param("userId") Long userId, @Param("months") int months);
    List<Map<String, Object>> getBiggestTransactions(@Param("userId") Long userId, @Param("limit") int limit);
    void deleteById(@Param("id") Long id, @Param("userId") Long userId);
    void updateImage(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("imageUrl") String imageUrl);
    Transaction findById(@Param("id") Long id, @Param("userId") Long userId);

    /** Returns all image_url values for a user so they can be purged from MinIO */
    List<String> findImageUrlsByUserId(@Param("userId") Long userId);

    /** Bulk-delete all transactions belonging to a user */
    void deleteByUserId(@Param("userId") Long userId);
}