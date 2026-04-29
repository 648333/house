package com.trae.housing.controller;

import com.trae.housing.model.PasswordResetToken;
import com.trae.housing.model.User;
import com.trae.housing.repository.PasswordResetTokenRepository;
import com.trae.housing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import com.trae.housing.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${trae.app.password-reset.code-expire-minutes:10}")
    private int passwordResetCodeExpireMinutes;

    @Value("${trae.app.password-reset.debug-return-code:true}")
    private boolean debugReturnCode;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(User.Role.USER); // Default role
        }
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest) {
        System.out.println("Login attempt for user: " + loginRequest.getUsername());
        try {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
            }

            // Auto-heal old demo data where enabled might be false.
            if (!user.isEnabled()) {
                user.setEnabled(true);
                userRepository.save(user);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("enabled", user.isEnabled());
            response.put("accessToken", jwt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload == null ? null : payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("message", "If this email exists, reset instructions have been sent"));
        }

        String code = generateResetCode();
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(normalizedEmail);
        token.setCode(code);
        token.setExpireAt(LocalDateTime.now().plusMinutes(Math.max(passwordResetCodeExpireMinutes, 1)));
        passwordResetTokenRepository.save(token);

        // Demo mode: no real email channel, print code in server log.
        System.out.println("Password reset code for " + normalizedEmail + ": " + code);

        if (debugReturnCode) {
            return ResponseEntity.ok(Map.of(
                    "message", "Reset code generated (demo mode)",
                    "code", code,
                    "expireMinutes", Math.max(passwordResetCodeExpireMinutes, 1)
            ));
        }

        return ResponseEntity.ok(Map.of("message", "If this email exists, reset instructions have been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload == null ? null : payload.get("email");
        String code = payload == null ? null : payload.get("code");
        String newPassword = payload == null ? null : payload.get("newPassword");

        if (email == null || email.isBlank() || code == null || code.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email, code and newPassword are required"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid reset request"));
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(normalizedEmail, code.trim())
                .orElse(null);
        if (token == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid code"));
        }

        if (token.getExpireAt() == null || LocalDateTime.now().isAfter(token.getExpireAt())) {
            return ResponseEntity.status(400).body(Map.of("message", "Code expired"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    private String generateResetCode() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", value);
    }
}
