package com.shigui.service.impl;

import com.shigui.dto.FileUploadResponse;
import com.shigui.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadRoot;
    private final String publicPrefix;
    private final Clock clock;

    public LocalFileStorageServiceImpl(
            @Value("${app.file.upload-dir:" + DEFAULT_UPLOAD_DIR + "}") String uploadDir,
            @Value("${app.file.public-prefix:" + DEFAULT_PUBLIC_PREFIX + "}") String publicPrefix) {
        this(Path.of(uploadDir), publicPrefix, Clock.systemDefaultZone());
    }

    public LocalFileStorageServiceImpl(Path uploadRoot, String publicPrefix, Clock clock) {
        this.uploadRoot = uploadRoot;
        this.publicPrefix = normalizePublicPrefix(publicPrefix);
        this.clock = clock;
    }

    @Override
    public FileUploadResponse storePostImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (!EXTENSIONS.containsKey(contentType)) {
            throw new IllegalArgumentException("只支持 JPG、PNG、WebP 图片上传");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 5MB");
        }
        validateImageContent(file);

        LocalDate today = LocalDate.now(clock);
        String extension = EXTENSIONS.get(contentType);
        String relativePath = Path.of(
                "posts",
                String.format("%04d", today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                UUID.randomUUID() + extension
        ).toString();
        Path targetPath = uploadRoot.resolve(relativePath);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setUrl(publicPrefix + "/" + relativePath.replace('\\', '/'));
        return response;
    }

    private void validateImageContent(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("上传文件内容不是有效图片");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("上传文件内容不是有效图片", e);
        }
    }

    private String normalizePublicPrefix(String prefix) {
        if (prefix == null || prefix.isBlank() || "/".equals(prefix)) {
            return DEFAULT_PUBLIC_PREFIX;
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
