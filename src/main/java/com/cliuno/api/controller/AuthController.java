package com.cliuno.api.controller;

import com.cliuno.api.entity.Role;
import com.cliuno.api.entity.User;
import com.cliuno.api.repo.*;
import com.cliuno.api.support.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final long HOUR = 3_600_000L;
    private static final long WEEK = 7 * 24 * HOUR;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RoleRepository roles;
    private final JwtService jwt;
    private final AuthSupport auth;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(
            UserRepository users, RoleRepository roles, JwtService jwt, AuthSupport auth) {
        this.users = users;
        this.roles = roles;
        this.jwt = jwt;
        this.auth = auth;
    }

    private static String randomToken() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private Map<String, Object> tokensFor(User user) {
        String token = jwt.sign(user.id, HOUR, false);
        String refreshToken = jwt.sign(user.id, WEEK, true);
        user.refreshToken = refreshToken;
        users.save(user);
        return Api.data("user", user, "token", token, "refreshToken", refreshToken);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        if (username == null || email == null || password == null
                || body.get("first_name") == null || body.get("last_name") == null) {
            throw new ApiException(400, "Missing required fields");
        }
        if (password.length() < 8) {
            throw new ApiException(400, "Password must be at least 8 characters");
        }
        if (users.findByUsername(username).isPresent() || users.findByEmail(email).isPresent()) {
            throw new ApiException(400, "User already exists");
        }

        // Default role is created on first use so a fresh install works out of the box
        Role role = roles.findByName("user").orElseGet(() -> {
            Role created = new Role();
            created.name = "user";
            return roles.save(created);
        });

        User user = new User();
        user.username = username;
        user.firstName = body.get("first_name");
        user.lastName = body.get("last_name");
        user.email = email;
        user.phone = body.get("phone");
        user.password = encoder.encode(password);
        user.role = role;
        // The verify token is stored so verify-email can look the user up by token later.
        user.verifyToken = randomToken();
        users.save(user);
        // In production, email the token; templates keep it local to the database.

        return Api.created(
                "User created successfully and an email has been sent to you for verification",
                Api.data("user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String login = body.get("usernameOrEmail");
        String password = body.get("password");
        if (login == null || password == null) {
            throw new ApiException(400, "usernameOrEmail and password are required");
        }

        User user = (login.contains("@") ? users.findByEmail(login) : users.findByUsername(login))
                .orElse(null);
        if (user == null || user.isDeleted || !encoder.matches(password, user.password)) {
            throw new ApiException(401, "Invalid username/email or password");
        }

        user.isOnline = true;
        return Api.ok("Login successful", tokensFor(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        User user = auth.require(request);
        user.isOnline = false;
        user.refreshToken = null;
        users.save(user);
        return Api.ok("Logout successful", null);
    }

    @PostMapping("/check-token")
    public ResponseEntity<Map<String, Object>> checkToken(HttpServletRequest request) {
        User user = auth.require(request);
        return Api.ok("Token is valid", Api.data("user", user));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
        // Frontends send the refresh token in the body; the header is a fallback
        String raw = body != null && body.get("refreshToken") != null
                ? body.get("refreshToken")
                : auth.bearer(request);
        Long userId = raw == null ? null : jwt.verify(raw, true);
        User user = userId == null ? null : users.findById(userId).orElse(null);
        if (user == null || !raw.equals(user.refreshToken)) {
            throw new ApiException(401, "Invalid or expired refresh token");
        }

        Map<String, Object> data = tokensFor(user);
        data.remove("user");
        return Api.ok("Token refreshed successfully", data);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        String oldPassword = body.getOrDefault("oldPassword", body.get("current_password"));
        String newPassword = body.getOrDefault("newPassword", body.get("new_password"));
        if (oldPassword == null || newPassword == null || newPassword.length() < 8) {
            throw new ApiException(400, "oldPassword and newPassword are required");
        }
        if (!encoder.matches(oldPassword, user.password)) {
            throw new ApiException(400, "Current password is incorrect");
        }

        user.password = encoder.encode(newPassword);
        users.save(user);
        return Api.ok("Password changed successfully", null);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> body) {
        // Frontends send `email`; `usernameOrEmail` is kept for compatibility
        String login = body.getOrDefault("email", body.get("usernameOrEmail"));
        if (login == null) {
            throw new ApiException(400, "Email is required");
        }
        (login.contains("@") ? users.findByEmail(login) : users.findByUsername(login))
                .ifPresent(user -> {
                    user.resetToken = randomToken();
                    users.save(user);
                    // In production, email the token; templates keep it local to the database.
                });
        return Api.ok("If the email exists, a reset link has been sent", null);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");
        User user = token == null ? null : users.findByResetToken(token).orElse(null);
        if (user == null) {
            throw new ApiException(400, "Invalid or expired reset token");
        }
        if (password == null || password.length() < 8) {
            throw new ApiException(400, "Password must be at least 8 characters");
        }

        user.password = encoder.encode(password);
        user.resetToken = null;
        users.save(user);
        return Api.ok("Password reset successful", null);
    }

    @PostMapping("/send-verify-email")
    public ResponseEntity<Map<String, Object>> sendVerifyEmail(HttpServletRequest request) {
        User user = auth.require(request);
        user.verifyToken = randomToken();
        users.save(user);
        // In production, email the token; templates keep it local to the database.
        return Api.ok("Email sent successfully", null);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        User user = token == null ? null : users.findByVerifyToken(token).orElse(null);
        if (user == null) {
            throw new ApiException(400, "Invalid or expired verification token");
        }
        user.isVerified = true;
        user.verifyToken = null;
        users.save(user);
        return Api.ok("Email verified successfully", null);
    }

    // OTP endpoints act on the authenticated user (RFC 6238 TOTP)
    @PostMapping("/otp/generate")
    public ResponseEntity<Map<String, Object>> otpGenerate(HttpServletRequest request) {
        User user = auth.require(request);
        String secret = TotpService.generateSecret();
        user.otpBase32 = secret;
        user.otpAuthUrl = TotpService.provisioningUri(secret, user.username);
        user.isOtpEnabled = false;
        users.save(user);
        return Api.ok(
                "OTP secret generated",
                Api.data("secret", secret, "base32", secret, "otpauth_url", user.otpAuthUrl));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> otpVerify(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        if (user.otpBase32 == null) {
            throw new ApiException(400, "OTP is not set up");
        }
        String code = body.getOrDefault("otp", body.get("token"));
        if (!TotpService.verify(user.otpBase32, code, 1)) {
            throw new ApiException(401, "Invalid OTP code");
        }
        user.isOtpEnabled = true;
        users.save(user);
        return Api.ok("OTP verified successfully", null);
    }

    @PostMapping("/otp/validate")
    public ResponseEntity<Map<String, Object>> otpValidate(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        if (user.otpBase32 == null || !user.isOtpEnabled) {
            throw new ApiException(400, "OTP is not enabled for this user");
        }
        String code = body.getOrDefault("otp", body.get("token"));
        if (!TotpService.verify(user.otpBase32, code, 1)) {
            throw new ApiException(401, "Invalid OTP code");
        }
        return Api.ok("Token is valid", null);
    }

    @PostMapping("/otp/disable")
    public ResponseEntity<Map<String, Object>> otpDisable(HttpServletRequest request) {
        User user = auth.require(request);
        user.isOtpEnabled = false;
        user.otpBase32 = null;
        user.otpAuthUrl = null;
        users.save(user);
        return Api.ok("OTP disabled successfully", null);
    }
}
