package com.cliuno.api.controller;

import com.cliuno.api.entity.User;
import com.cliuno.api.repo.*;
import com.cliuno.api.support.Api;
import com.cliuno.api.support.ApiException;
import com.cliuno.api.support.AuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    private final PostRepository posts;
    private final AuthSupport auth;

    public UserController(UserRepository users, PostRepository posts, AuthSupport auth) {
        this.users = users;
        this.posts = posts;
        this.auth = auth;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(404, "User not found"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> index(HttpServletRequest request) {
        auth.require(request);
        return Api.ok("Users", Api.data("users", users.findAll()));
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> current(HttpServletRequest request) {
        return Api.ok("Current user", Api.data("user", auth.require(request)));
    }

    @PatchMapping("/current")
    public ResponseEntity<Map<String, Object>> updateCurrent(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        applyProfileChanges(user, body);
        users.save(user);
        return Api.ok("User updated", Api.data("user", user));
    }

    @DeleteMapping("/current")
    public ResponseEntity<Map<String, Object>> deleteCurrent(HttpServletRequest request) {
        User user = auth.require(request);
        user.isDeleted = true;
        users.save(user);
        return Api.ok("User deleted", Api.data("user", user));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> byUsername(
            HttpServletRequest request, @PathVariable String username) {
        auth.require(request);
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(404, "User not found"));
        return Api.ok("User", Api.data("user", user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> show(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        return Api.ok("User", Api.data("user", find(id)));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<Map<String, Object>> userPosts(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        find(id);
        return Api.ok("Posts", Api.data("posts", posts.findByUserIdOrderByCreatedAtDesc(id)));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<Map<String, Object>> userRoles(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        return Api.ok("Role found", Api.data("role", find(id).role));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        auth.requireAdmin(auth.require(request));
        User user = find(id);
        applyProfileChanges(user, body);
        users.save(user);
        return Api.ok("User updated", Api.data("user", user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> destroy(
            HttpServletRequest request, @PathVariable Long id) {
        auth.requireAdmin(auth.require(request));
        User user = find(id);
        user.isDeleted = true;
        users.save(user);
        return Api.ok("User deleted", Api.data("user", user));
    }

    private static void applyProfileChanges(User user, Map<String, String> body) {
        if (body.get("first_name") != null) {
            user.firstName = body.get("first_name");
        }
        if (body.get("last_name") != null) {
            user.lastName = body.get("last_name");
        }
        if (body.get("phone") != null) {
            user.phone = body.get("phone");
        }
    }
}
