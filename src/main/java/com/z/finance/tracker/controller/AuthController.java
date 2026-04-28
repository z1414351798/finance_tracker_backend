package com.z.finance.tracker.controller;

import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil; // Inject the utility

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body("User already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        // 1. Find user in DB
        User user = userMapper.findByUsername(loginRequest.getUsername());

        // 2. Verify password
        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 3. Generate real JWT
            String token = jwtUtil.generateToken(user.getUsername());

            // Return as a JSON object so frontend can easily parse it
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(401).body("Invalid username or password");
    }
}