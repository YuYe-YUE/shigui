package com.shigui.config;

import com.shigui.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcResourceConfig implements WebMvcConfigurer {

    private final String publicPrefix;
    private final Path uploadRoot;

    public WebMvcResourceConfig(
            @Value("${app.file.upload-dir:" + FileStorageService.DEFAULT_UPLOAD_DIR + "}") String uploadDir,
            @Value("${app.file.public-prefix:" + FileStorageService.DEFAULT_PUBLIC_PREFIX + "}") String publicPrefix) {
        this.publicPrefix = normalizePublicPrefix(publicPrefix);
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(uploadRoot.toUri().toString());
    }

    private String normalizePublicPrefix(String prefix) {
        if (prefix == null || prefix.isBlank() || "/".equals(prefix)) {
            return FileStorageService.DEFAULT_PUBLIC_PREFIX;
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
