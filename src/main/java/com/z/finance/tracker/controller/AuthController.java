package com.z.finance.tracker.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.z.finance.tracker.entity.User;
import com.z.finance.tracker.mapper.UserMapper;
import com.z.finance.tracker.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null) {
            log.warn("Google login attempt with missing token");
            return ResponseEntity.badRequest().body("Missing token");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new com.google.api.client.http.javanet.NetHttpTransport(),
                    new com.google.api.client.json.gson.GsonFactory()
            )
                    .setAudience(List.of(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.warn("Google login failed: invalid token");
                return ResponseEntity.status(401).body("Invalid Google token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            User user = userMapper.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setUsername(name != null ? name : email.split("@")[0]);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                userMapper.insert(user);
                log.info("Google login: new user created [email={}]", email);
            } else {
                log.info("Google login: existing user [email={}]", email);
            }

            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of("token", token));

        } catch (Exception e) {
            log.error("Google auth error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Google auth failed: " + e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            log.warn("Signup failed: username '{}' already exists", user.getUsername());
            return ResponseEntity.badRequest().body("User already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        log.info("New user registered: [username={}]", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        log.debug("Login attempt: [username={}]", loginRequest.getUsername());
        User user = userMapper.findByUsername(loginRequest.getUsername());

        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            String token = jwtUtil.generateToken(user.getUsername());
            log.info("Login success: [username={}]", user.getUsername());
            return ResponseEntity.ok(Map.of("token", token));
        }

        log.warn("Login failed: invalid credentials for [username={}]", loginRequest.getUsername());
        return ResponseEntity.status(401).body("Invalid username or password");
    }
}
