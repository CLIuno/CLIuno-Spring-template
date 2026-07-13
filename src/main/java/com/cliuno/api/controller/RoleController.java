package com.cliuno.api.controller;

import com.cliuno.api.entity.Role;
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
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleRepository roles;
    private final AuthSupport auth;

    public RoleController(RoleRepository roles, AuthSupport auth) {
        this.roles = roles;
        this.auth = auth;
    }

    private User admin(HttpServletRequest request) {
        User user = auth.require(request);
        auth.requireAdmin(user);
        return user;
    }

    private Role find(Long id) {
        return roles.findById(id).orElseThrow(() -> new ApiException(404, "Role not found"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> index(HttpServletRequest request) {
        admin(request);
        return Api.ok("Roles", Api.data("roles", roles.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> show(
            HttpServletRequest request, @PathVariable Long id) {
        admin(request);
        return Api.ok("Role", Api.data("role", find(id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> store(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        admin(request);
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new ApiException(400, "Role name is required");
        }
        if (roles.findByName(name).isPresent()) {
            throw new ApiException(400, "Role already exists");
        }
        Role role = new Role();
        role.name = name;
        roles.save(role);
        return Api.created("Role created successfully", Api.data("role", role));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        admin(request);
        Role role = find(id);
        if (body.get("name") != null) {
            role.name = body.get("name");
        }
        roles.save(role);
        return Api.ok("Role updated", Api.data("role", role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> destroy(
            HttpServletRequest request, @PathVariable Long id) {
        admin(request);
        roles.delete(find(id));
        return Api.ok("Role deleted successfully", null);
    }
}
