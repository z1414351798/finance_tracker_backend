package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.RecurringTransaction;
import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.mapper.RecurringMapper;
import com.z.finance.tracker.mapper.TransactionMapper;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.CacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
public class RecurringController {

    private static final Logger log = LoggerFactory.getLogger(RecurringController.class);

    @Autowired private RecurringMapper recurringMapper;
    @Autowired private TransactionMapper transactionMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CacheInvalidationService cacheInvalidation;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @GetMapping
    public List<RecurringTransaction> getAll() {
        return recurringMapper.findByUserId(getCurrentUserId());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody RecurringTransaction r) {
        r.setUserId(getCurrentUserId());
        if (r.getIsActive() == null) r.setIsActive(true);
        recurringMapper.insert(r);
        log.info("Recurring added [userId={}, text='{}', frequency={}]",
                r.getUserId(), r.getText(), r.getFrequency());
        return ResponseEntity.ok(r);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody RecurringTransaction r) {
        r.setId(id);
        r.setUserId(getCurrentUserId());
        recurringMapper.update(r);
        return ResponseEntity.ok(Map.of("message", "Updated"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        recurringMapper.deleteById(id, userId);
        log.info("Recurring deleted [userId={}, id={}]", userId, id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // ── Upcoming (home screen banner) ─────────────────────────────────────────

    @GetMapping("/upcoming")
    public List<RecurringTransaction> upcoming(
            @RequestParam(defaultValue = "7") int days) {
        return recurringMapper.findUpcoming(getCurrentUserId(), days);
    }

    // ── Pay now ───────────────────────────────────────────────────────────────
    // Creates a real transaction and advances next_due_date by one frequency period.

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        List<RecurringTransaction> all = recurringMapper.findByUserId(userId);
        RecurringTransaction r = all.stream()
                .filter(x -> x.getId().equals(id))
                .findFirst().orElse(null);

        if (r == null) return ResponseEntity.notFound().build();

        // 1. Create the real transaction
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setText(r.getText());
        tx.setAmount("EXPENSE".equals(r.getType()) ? -Math.abs(r.getAmount())
                                                    :  Math.abs(r.getAmount()));
        tx.setType(com.z.finance.tracker.enums.TraType.valueOf(r.getType()));
        tx.setCategoryId(r.getCategoryId() != null ? r.getCategoryId() : 0L);
        tx.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        tx.setNote(r.getNote() != null ? r.getNote() : "");
        transactionMapper.insert(tx);
        cacheInvalidation.evictAllUserCaches(userId);

        // 2. Advance next_due_date
        String nextDue = advanceDate(r.getNextDueDate(), r.getFrequency());
        recurringMapper.updateNextDueDate(id, userId, nextDue);

        log.info("Recurring paid [userId={}, id={}, text='{}', nextDue={}]",
                userId, id, r.getText(), nextDue);
        return ResponseEntity.ok(Map.of(
                "message",     "Paid and next due date updated",
                "nextDueDate", nextDue,
                "transactionId", tx.getId()
        ));
    }

    // ── Auto-detect ───────────────────────────────────────────────────────────

    @GetMapping("/detect")
    public List<Map<String, Object>> detect() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> patterns = recurringMapper.detectPatterns(userId);

        // Enrich each pattern with a suggested frequency and nextDueDate
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Map<String, Object> p : patterns) {
            int interval = ((Number) p.get("avgIntervalDays")).intValue();
            String lastDate = (String) p.get("lastDate");
            String freq = toFrequency(interval);
            String nextDue = advanceDate(lastDate, freq);
            p.put("suggestedFrequency", freq);
            p.put("suggestedNextDueDate", nextDue);
        }
        return patterns;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String advanceDate(String dateStr, String frequency) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDate next = switch (frequency) {
            case "WEEKLY"     -> date.plusWeeks(1);
            case "BIWEEKLY"   -> date.plusWeeks(2);
            case "QUARTERLY"  -> date.plusMonths(3);
            case "YEARLY"     -> date.plusYears(1);
            default           -> date.plusMonths(1); // MONTHLY
        };
        // If still in the past, keep advancing
        while (next.isBefore(LocalDate.now())) {
            next = advanceLocalDate(next, frequency);
        }
        return next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private LocalDate advanceLocalDate(LocalDate date, String frequency) {
        return switch (frequency) {
            case "WEEKLY"    -> date.plusWeeks(1);
            case "BIWEEKLY"  -> date.plusWeeks(2);
            case "QUARTERLY" -> date.plusMonths(3);
            case "YEARLY"    -> date.plusYears(1);
            default          -> date.plusMonths(1);
        };
    }

    private String toFrequency(int avgDays) {
        if (avgDays <= 10)  return "WEEKLY";
        if (avgDays <= 20)  return "BIWEEKLY";
        if (avgDays <= 50)  return "MONTHLY";
        if (avgDays <= 120) return "QUARTERLY";
        return "YEARLY";
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        var user = userMapper.findByUsername(username);
        return user != null ? user.getId() : null;
    }
}
