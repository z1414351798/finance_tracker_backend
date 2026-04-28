package com.z.finance.tracker.service;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {
    @Autowired
    private TransactionMapper transactionMapper;

    public List<DailyTrendDTO> getDailyTrendsForUser(Long userId, int days) {
        return transactionMapper.getDailyTrends(userId, days);
    }
}