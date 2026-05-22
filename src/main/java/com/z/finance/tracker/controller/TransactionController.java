package com.z.finance.tracker.controller;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.dto.SummaryDTO;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.TransactionMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.CacheInvalidationService;
import com.z.finance.tracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    @Autowired private TransactionService transactionService;
    @Autowired private CacheInvalidationService cacheInvalidation;

    public TransactionController(TransactionMapper transactionMapper, UserMapper userMapper) {
        this.transactionMapper = transactionMapper;
        this.userMapper = userMapper;
    }

    @PostMapping("/add")
    public Transaction create(@RequestBody Transaction transaction) {
        Long userId = getCurrentUserId();
        transaction.setUserId(userId);
        transactionMapper.insert(transaction);
        cacheInvalidation.evictAllUserCaches(userId);
        log.info("Transaction created [userId={}, text='{}', amount={}, type={}]",
                userId, transaction.getText(), transaction.getAmount(), transaction.getType());
        return transaction;
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @RequestBody Transaction transaction) {
        Long userId = getCurrentUserId();
        transaction.setId(id);
        transaction.setUserId(userId);
        transactionMapper.update(transaction);
        cacheInvalidation.evictAllUserCaches(userId);
        log.info("Transaction updated [userId={}, id={}]", userId, id);
        return ResponseEntity.ok(Map.of("message", "Updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        transactionMapper.deleteById(id, userId);
        cacheInvalidation.evictAllUserCaches(userId);
        log.info("Transaction deleted [userId={}, id={}]", userId, id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }


    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TraType type,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String note
    ) throws Exception {
        Long userId = getCurrentUserId();
        int offset = page * size;

        List<Transaction> list = transactionMapper.findAllFiltered(
                userId, offset, size, text, categoryId, type,
                minAmount, maxAmount, startDate, endDate, note);

        long total = transactionMapper.countFiltered(
                userId, text, categoryId, type,
                minAmount, maxAmount, startDate, endDate, note);

        log.debug("History query [userId={}, page={}, size={}, total={}]", userId, page, size, total);

        Map<String, Object> response = new HashMap<>();
        response.put("content", list);
        response.put("totalElements", total);
        return response;
    }

    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrendDTO>> getDailyTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(transactionService.getDailyTrendsForUser(getCurrentUserId(), days));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryDTO> getSummary(
            @RequestParam(defaultValue = "") String month) {
        return ResponseEntity.ok(transactionService.getSummary(getCurrentUserId(), month));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<Map<String, Object>>> getTrends(
            @RequestParam String range,
            @RequestParam TraType type) {
        return ResponseEntity.ok(transactionService.getTrends(getCurrentUserId(), range, type));
    }

    @GetMapping("/monthly-breakdown")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyBreakdown(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(transactionMapper.getMonthlyBreakdown(getCurrentUserId(), months));
    }

    @GetMapping("/biggest-transactions")
    public ResponseEntity<List<Map<String, Object>>> getBiggestTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(transactionMapper.getBiggestTransactions(getCurrentUserId(), limit));
    }

    // GET /api/transactions/export/csv
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).build();

        List<Transaction> transactions = transactionMapper.findAllByUserId(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Name,Category,Amount,Note\n");
        for (Transaction t : transactions) {
            csv.append(escapeCsv(t.getDate())).append(",")
               .append(escapeCsv(t.getType() != null ? t.getType().name() : "")).append(",")
               .append(escapeCsv(t.getText())).append(",")
               .append(escapeCsv(t.getCategoryName())).append(",")
               .append(t.getAmount() != null ? t.getAmount() : "").append(",")
               .append(escapeCsv(t.getNote())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        // BOM so Excel opens it correctly
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] output = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, output, 0, bom.length);
        System.arraycopy(bytes, 0, output, bom.length, bytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(output);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userMapper.findByUsername(username);
        return (user != null) ? user.getId() : null;
    }
}
