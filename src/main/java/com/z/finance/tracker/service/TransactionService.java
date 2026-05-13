package com.z.finance.tracker.service;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.dto.SummaryDTO;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    @Autowired private TransactionMapper transactionMapper;

    @Cacheable(value = "dailyTrends", key = "'days:' + #days + ':user:' + #userId")
    public List<DailyTrendDTO> getDailyTrendsForUser(Long userId, int days) {
        return transactionMapper.getDailyTrends(userId, days);
    }

    @Cacheable(value = "summary", key = "'month:' + #month + ':user:' + #userId")
    public SummaryDTO getSummary(Long userId, String month) {
        String targetMonth = (month == null || month.isEmpty())
                ? new SimpleDateFormat("yyyy-MM").format(new Date())
                : month;

        return new SummaryDTO(
                transactionMapper.getTotalBalance(userId),
                transactionMapper.getMonthlyBalance(userId, targetMonth),
                transactionMapper.getIncomeVsExpense(userId),
                transactionMapper.getCategorySummary(userId, TraType.INCOME),
                transactionMapper.getCategorySummary(userId, TraType.EXPENSE)
        );
    }

    @Cacheable(value = "trends", key = "'range:' + #range + ':type:' + #type + ':user:' + #userId")
    public List<Map<String, Object>> getTrends(Long userId, String range, TraType type) {
        return transactionMapper.getTimeSeriesData(userId, range, type);
    }
}