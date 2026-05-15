package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.UserConsent;
import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.ConsentMapper;
import com.z.finance.tracker.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private static final Logger log = LoggerFactory.getLogger(ConsentController.class);

    @Autowired private ConsentMapper consentMapper;
    @Autowired private UserMapper userMapper;

    @PostMapping
    public ResponseEntity<?> recordConsent(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) return ResponseEntity.status(401).body("User not found");

        UserConsent consent = new UserConsent();
        consent.setUserId(user.getId());
        consent.setPolicyVersion(body.getOrDefault("policyVersion", "2026-05"));
        consent.setPlatform(body.getOrDefault("platform", "unknown"));
        consent.setAcceptedAt(LocalDateTime.now());

        // Get real IP (handle proxies)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        consent.setIpAddress(ip);

        consentMapper.insert(consent);
        log.info("Consent recorded [userId={}, platform={}, version={}]",
                user.getId(), consent.getPlatform(), consent.getPolicyVersion());

        return ResponseEntity.ok(Map.of("message", "Consent recorded"));
    }

    @GetMapping
    public ResponseEntity<?> getConsents() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) return ResponseEntity.status(401).body("User not found");
        List<UserConsent> consents = consentMapper.findByUserId(user.getId());
        return ResponseEntity.ok(consents);
    }
}
