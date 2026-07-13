package com.cliuno.api.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(
            @Value("${cliuno.jwt.secret}") String secret,
            @Value("${cliuno.jwt.refresh-secret}") String refreshSecret) {
        this.accessKey = Keys.hmacShaKeyFor(pad(secret));
        this.refreshKey = Keys.hmacShaKeyFor(pad(refreshSecret));
    }

    private static byte[] pad(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }

    public String sign(Long userId, long ttlMillis, boolean refresh) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(refresh ? refreshKey : accessKey)
                .compact();
    }

    public Long verify(String token, boolean refresh) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(refresh ? refreshKey : accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
