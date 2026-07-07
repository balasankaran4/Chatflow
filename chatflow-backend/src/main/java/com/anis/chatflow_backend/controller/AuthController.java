package com.anis.chatflow_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anis.chatflow_backend.dto.AuthResponse;
import com.anis.chatflow_backend.dto.LoginRequest;
import com.anis.chatflow_backend.dto.ProfileResponse;
import com.anis.chatflow_backend.dto.ProfileUpdateRequest;
import com.anis.chatflow_backend.dto.RegisterRequest;
import com.anis.chatflow_backend.dto.UserActionRequest;
import com.anis.chatflow_backend.dto.UserSummaryResponse;
import com.anis.chatflow_backend.model.User;
import com.anis.chatflow_backend.security.JwtUtil;
import com.anis.chatflow_backend.service.UserService;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/")
    public String home() {
        return "Auth API Working";
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.ok(Map.of("message", "User registered successfully."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        try {
            User user = userService.authenticate(request.email(), request.password());
            UserSummaryResponse summary = userService.toSummary(user, user);
            String token = jwtUtil.generateToken(summary.email());
            return ResponseEntity.ok(new AuthResponse(token, summary));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getUsers(@RequestParam String currentUser) {
        return ResponseEntity.ok(userService.getUsersFor(currentUser));
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@RequestParam String email) {
        return ResponseEntity.ok(userService.getProfile(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request) {
        try {
            return ResponseEntity.ok(userService.updateProfile(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/requests/send")
    public ResponseEntity<?> sendRequest(@RequestBody UserActionRequest request) {
        return executeProfileAction(() -> userService.sendRequest(request));
    }

    @PostMapping("/requests/accept")
    public ResponseEntity<?> acceptRequest(@RequestBody UserActionRequest request) {
        return executeProfileAction(() -> userService.acceptRequest(request));
    }

    @PostMapping("/requests/reject")
    public ResponseEntity<?> rejectRequest(@RequestBody UserActionRequest request) {
        return executeProfileAction(() -> userService.rejectRequest(request));
    }

    @PostMapping("/block")
    public ResponseEntity<?> blockUser(@RequestBody UserActionRequest request) {
        return executeProfileAction(() -> userService.blockUser(request));
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblockUser(@RequestBody UserActionRequest request) {
        return executeProfileAction(() -> userService.unblockUser(request));
    }

    private ResponseEntity<?> executeProfileAction(ProfileSupplier supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @FunctionalInterface
    private interface ProfileSupplier {
        ProfileResponse get();
    }
}
