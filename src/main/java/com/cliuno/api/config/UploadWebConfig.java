package com.cliuno.api.config;

import com.cliuno.api.controller.UploadController;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded images publicly at /uploads/<filename> — no /api/v1 prefix, no auth. */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(UploadController.ensureUploadDir().toUri().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare the uploads directory", e);
        }
    }
}
