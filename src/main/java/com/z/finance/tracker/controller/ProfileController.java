package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.Transaction;
import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.service.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.z.finance.tracker.service.MinioStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    // Set this in application.properties: app.upload-dir=./uploads
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Autowired private MinioStorageService minioStorage;

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("image") MultipartFile file) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        if (file.isEmpty()) return ResponseEntity.badRequest().body("No file provided");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("File must be an image");
        }

        try {
            // Delete old avatar
            if (user.getProfileImageUrl() != null) {
                minioStorage.delete(user.getProfileImageUrl());
            }

            // Build unique object name
            String extension = getExtension(file.getOriginalFilename());
            String objectName = "avatars/avatar_"
                    + user.getId() + "_"
                    + UUID.randomUUID()
                    + extension;

            // Upload to MinIO
            String imageUrl = minioStorage.upload(file, objectName);

            // Save URL to DB
            user.setProfileImageUrl(imageUrl);
            userMapper.updateProfile(user);

            log.info("Avatar uploaded [userId={}, object={}]", user.getId(), objectName);
            return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl));

        } catch (Exception e) {
            log.error("Avatar upload failed [userId={}, error={}]", user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    // GET /api/profile/avatar  — authenticated image proxy
    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar() {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getProfileImageUrl() == null) return ResponseEntity.notFound().build();
        try {
            byte[] bytes = minioStorage.getBytes(user.getProfileImageUrl());
            String ct = MinioStorageService.contentTypeFor(user.getProfileImageUrl());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ct))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/avatar-url")
    public ResponseEntity<String> getTransactionImageUrl(@PathVariable Long id) {

        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        if (user.getProfileImageUrl() == null) return ResponseEntity.notFound().build();

        try {
            String url = minioStorage.generatePresignedUrl(user.getProfileImageUrl());
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("Failed to generate url", e);
            return ResponseEntity.internalServerError().build();
        }
    }

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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user.getProfileImageUrl() != null) {
            try {
                user.setPresignedImageUrl(minioStorage.generatePresignedUrl(user.getProfileImageUrl()));
            } catch (Exception e) {
                log.warn("Could not generate presigned avatar URL [userId={}]: {}", user.getId(), e.getMessage());
            }
        }
        return user;
    }
}