package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired private UserMapper userMapper;
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
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // POST /api/profile/avatar
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("image") MultipartFile file) {
        User user = getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file provided");
        }

        // Validate it's an image
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("File must be an image");
        }

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Delete old avatar if it exists and is a local file
            if (user.getProfileImageUrl() != null && user.getProfileImageUrl().startsWith("/uploads/")) {
                String oldFileName = user.getProfileImageUrl().replace("/uploads/", "");
                Path oldFile = uploadPath.resolve(oldFileName);
                Files.deleteIfExists(oldFile);
            }

            // Save new file with unique name
            String extension = getExtension(file.getOriginalFilename());
            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // Save URL to DB (relative path, served as static resource)
            String imageUrl = "/uploads/" + fileName;
            user.setProfileImageUrl(imageUrl);
            userMapper.updateProfile(user);

            return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload image");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userMapper.findByUsername(username);
    }
}