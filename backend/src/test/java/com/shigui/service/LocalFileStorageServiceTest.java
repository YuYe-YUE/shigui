package com.shigui.service;

import com.shigui.dto.FileUploadResponse;
import com.shigui.service.impl.LocalFileStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storePostImage_png_success() throws IOException {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-card.png",
                "image/png",
                createPngBytes()
        );

        FileUploadResponse response = service.storePostImage(file);

        assertTrue(response.getUrl().matches("^/uploads/posts/2026/05/18/[0-9a-f\\-]+\\.png$"));
        assertTrue(service.isStoredPostImage(response.getUrl()));
        Path storedFile = tempDir.resolve(response.getUrl().replaceFirst("^/uploads/", ""));
        assertTrue(Files.exists(storedFile));
        assertTrue(Files.size(storedFile) > 0);
    }

    @Test
    void storePostImage_nonImage_throwsIllegalArgumentException() {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.storePostImage(file));

        assertEquals("只支持 JPG、PNG、WebP 图片上传", exception.getMessage());
    }

    @Test
    void storePostImage_forgedPngContent_throwsIllegalArgumentException() {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "forged.png",
                "image/png",
                "not-a-real-image".getBytes()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.storePostImage(file));

        assertEquals("上传文件内容不是有效图片", exception.getMessage());
    }

    @Test
    void storePostImage_webp_success() throws IOException {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-card.webp",
                "image/webp",
                createWebpBytes()
        );

        FileUploadResponse response = service.storePostImage(file);

        assertTrue(response.getUrl().matches("^/uploads/posts/2026/05/18/[0-9a-f\\-]+\\.webp$"));
        Path storedFile = tempDir.resolve(response.getUrl().replaceFirst("^/uploads/", ""));
        assertTrue(Files.exists(storedFile));
        assertTrue(Files.size(storedFile) > 0);
    }

    @Test
    void isStoredPostImage_nonexistentFile_returnsFalse() {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );

        assertTrue(!service.isStoredPostImage("/uploads/posts/2026/05/18/missing.png"));
    }

    @Test
    void isStoredPostImage_pathTraversal_returnsFalse() {
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(
                tempDir,
                "/uploads",
                Clock.fixed(Instant.parse("2026-05-18T08:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );

        assertTrue(!service.isStoredPostImage("/uploads/posts/../../secret.txt"));
    }

    private byte[] createPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private byte[] createWebpBytes() {
        return Base64.getDecoder().decode(
                "UklGRkAAAABXRUJQVlA4IDQAAADwAQCdASoBAAEAAQAcJaACdLoB+AAETAAA/vW4f/6aR40jxpHxcP/ugT90CfugT/3NoAAA"
        );
    }
}
