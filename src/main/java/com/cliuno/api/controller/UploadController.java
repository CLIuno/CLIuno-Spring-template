package com.cliuno.api.controller;

import com.cliuno.api.support.Api;
import com.cliuno.api.support.ApiException;
import com.cliuno.api.support.AuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    /** 5 MB — matches every other CLIuno backend template. */
    public static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    /** Uploaded files live at the repo root and are served back from /uploads. */
    public static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads");

    /** The extension comes from the detected mime, never from the client's filename. */
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final AuthSupport auth;

    public UploadController(AuthSupport auth) {
        this.auth = auth;
    }

    public static Path ensureUploadDir() throws IOException {
        Files.createDirectories(UPLOAD_DIR);
        return UPLOAD_DIR;
    }

    /**
     * Stores the bytes and hands back a URL. Attaching it to a user or a post is the
     * caller's job (PATCH /users/current, POST /posts) — upload mutates nothing.
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> image(
            HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) {
        auth.require(request);

        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "No image file provided");
        }

        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new ApiException(400, "Unsupported image type: " + file.getContentType());
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(413, "Image exceeds the 5 MB limit");
        }

        String filename = UUID.randomUUID() + extension;
        try {
            Files.write(ensureUploadDir().resolve(filename), file.getBytes());
        } catch (IOException e) {
            throw new ApiException(500, "Could not store the uploaded image");
        }

        // Absolute so a frontend on another origin can use it straight as an image source.
        String configured = System.getenv("PUBLIC_BASE_URL");
        String base = configured != null && !configured.isBlank()
                ? configured
                : request.getScheme() + "://" + request.getHeader("Host");
        base = base.replaceAll("/+$", "");

        return Api.created(
                "Image uploaded", Api.data("url", base + "/uploads/" + filename, "filename", filename));
    }
}
