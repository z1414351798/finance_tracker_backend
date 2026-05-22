package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired private UserMapper userMapper;
    @Autowired private TransactionMapper transactionMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private BudgetMapper budgetMapper;
    @Autowired private GoalMapper goalMapper;
    @Autowired private RecurringMapper recurringMapper;
    @Autowired private ConsentMapper consentMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    // Set this in application.properties: app.upload-dir=./uploads
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    // GET /api/profile
    @GetMapping
    public ResponseEntity<?> getProfile() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        // Never send password back
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // PUT /api/profile
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        String email = body.get("email");
        // Only update fields that were sent
        user.setEmail(email);
        // profileImageUrl stays as-is unless changed via upload endpoint
        userMapper.updateProfile(user);

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // PUT /api/profile/password
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Invalid password data");
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(403).body("Current password is incorrect");
        }

        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        log.info("Password changed [userId={}]", user.getId());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // DELETE /api/profile/account  — permanently delete account + all data
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        Long userId = user.getId();
        log.info("Account deletion requested [userId={}]", userId);

        try {

            // 3. Delete DB rows in safe order (budgets before categories due to FK)
            budgetMapper.deleteByUserId(userId);
            goalMapper.deleteByUserId(userId);
            recurringMapper.deleteByUserId(userId);
            transactionMapper.deleteByUserId(userId);
            categoryMapper.deleteByUserId(userId);
            consentMapper.deleteByUserId(userId);

            // 4. Delete the user record
            userMapper.deleteById(userId);

            log.info("Account deleted [userId={}]", userId);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));

        } catch (Exception e) {
            log.error("Account deletion failed [userId={}, error={}]", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to delete account");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        return user;
    }
}