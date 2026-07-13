package com.cliuno.api.support;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Builders for the shared CLIuno response envelope: {status, message, data}. */
public final class Api {
    private Api() {}

    public static ResponseEntity<Map<String, Object>> ok(String message, Map<String, Object> data) {
        return envelope(HttpStatus.OK, "success", message, data);
    }

    public static ResponseEntity<Map<String, Object>> created(
            String message, Map<String, Object> data) {
        return envelope(HttpStatus.CREATED, "success", message, data);
    }

    public static ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message) {
        return envelope(status, status.is4xxClientError() ? "error" : "error", message, null);
    }

    public static Map<String, Object> data(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static ResponseEntity<Map<String, Object>> envelope(
            HttpStatus status, String state, String message, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", state);
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        return ResponseEntity.status(status).body(body);
    }
}
