package com.cliuno.api.support;

import com.cliuno.api.entity.User;
import com.cliuno.api.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthSupport {
    private final JwtService jwt;
    private final UserRepository users;

    public AuthSupport(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    public String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    /** Resolve the Bearer user or throw the 401 the exception handler turns into an envelope. */
    public User require(HttpServletRequest request) {
        String token = bearer(request);
        Long userId = token == null ? null : jwt.verify(token, false);
        User user = userId == null ? null : users.findById(userId).orElse(null);
        if (user == null) {
            throw new ApiException(401, "No token provided or token is invalid");
        }
        return user;
    }

    public void requireAdmin(User user) {
        if (user.role == null || !"admin".equals(user.role.name)) {
            throw new ApiException(403, "Forbidden: Permission denied");
        }
    }
}
