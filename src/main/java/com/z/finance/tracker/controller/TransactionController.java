package com.z.finance.tracker.controller;

import com.z.finance.tracker.dto.DailyTrendDTO;
import com.z.finance.tracker.dto.SummaryDTO;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.enums.TraType;
import com.z.finance.tracker.mapper.TransactionMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.CacheInvalidationService;
import com.z.finance.tracker.service.MinioStorageService;
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

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;

    @Autowired private TransactionService transactionService;
    @Autowired private CacheInvalidationService cacheInvalidation;
    @Autowired private MinioStorageService minioStorage;

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

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadTransactionImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {
        Long userId = getCurrentUserId();

        if (file.isEmpty()) return ResponseEntity.badRequest().body("No file");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("File must be an image");
        }

        try {
            Transaction existing = transactionMapper.findById(id, userId);
            if (existing != null && existing.getImageUrl() != null) {
                minioStorage.delete(existing.getImageUrl());
            }

            String extension = getExtension(file.getOriginalFilename());
            String objectName = "transactions/tx_" + id + "_" + UUID.randomUUID() + extension;
            String imageUrl = minioStorage.upload(file, objectName);

            transactionMapper.updateImage(id, userId, imageUrl);
            cacheInvalidation.evictAllUserCaches(userId);

            log.info("Image uploaded [userId={}, txId={}, object={}]", userId, id, objectName);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            log.error("Image upload failed [userId={}, txId={}, error={}]", userId, id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<?> deleteTransactionImage(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        try {
            Transaction existing = transactionMapper.findById(id, userId);
            if (existing != null && existing.getImageUrl() != null) {
                minioStorage.delete(existing.getImageUrl());
                transactionMapper.updateImage(id, userId, null);
                cacheInvalidation.evictAllUserCaches(userId);
                log.info("Transaction image deleted [userId={}, txId={}]", userId, id);
            }
            return ResponseEntity.ok(Map.of("message", "Image deleted"));
        } catch (Exception e) {
            log.error("Image delete failed [userId={}, txId={}, error={}]", userId, id, e.getMessage());
            return ResponseEntity.internalServerError().body("Delete failed");
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getTransactionImage(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Transaction tx = transactionMapper.findById(id, userId);
        if (tx == null || tx.getImageUrl() == null) return ResponseEntity.notFound().build();
        try {
            byte[] bytes = minioStorage.getBytes(tx.getImageUrl());
            String ct = MinioStorageService.contentTypeFor(tx.getImageUrl());
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(bytes);
        } catch (Exception e) {
            log.error("Image fetch failed [userId={}, txId={}, error={}]", userId, id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/image-url")
    public ResponseEntity<String> getTransactionImageUrl(@PathVariable Long id) {

        Long userId = getCurrentUserId();

        Transaction tx = transactionMapper.findById(id, userId);

        if (tx == null || tx.getImageUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String url = minioStorage.generatePresignedUrl(tx.getImageUrl());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("Failed to generate url", e);
            return ResponseEntity.internalServerError().build();
        }
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

        // Attach presigned URLs — Android loads images directly via nginx→MinIO (fast)
        for (Transaction tx : list) {
            if (tx.getImageUrl() != null) {
                try {
                    tx.setImagePresignedUrl(minioStorage.generatePresignedUrl(tx.getImageUrl()));
                } catch (Exception e) {
                    log.warn("Could not generate presigned URL for tx={}: {}", tx.getId(), e.getMessage());
                }
            }
        }

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
