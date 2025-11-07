package edu.xtu.bbs.user.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import edu.xtu.bbs.user.config.AvatarConfiguration;
import edu.xtu.bbs.user.config.OssConfiguration;
import edu.xtu.bbs.user.exception.IllegalContentTypeException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AvatarServiceTest {

    private AvatarService avatarService;

    private String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    @BeforeEach
    void setUp() {
        final OssConfiguration ossConfiguration = new OssConfiguration();
        final String tencentSecretId = getEnvOrDefault("TENCENT_SECRET_ID", "id");
        final String tencentSecretKey = getEnvOrDefault("TENCENT_SECRET_KEY", "key");
        final String tencentRegion = getEnvOrDefault("TENCENT_REGION", "ap-guangzhou");
        final String tencentBucket = getEnvOrDefault("TENCENT_BUCKET", "avatar-bucket");
        ossConfiguration.setRegion(tencentRegion);
        ossConfiguration.setSecretId(tencentSecretId);
        ossConfiguration.setSecretKey(tencentSecretKey);
        final AvatarConfiguration avatarConfiguration = new AvatarConfiguration();
        avatarConfiguration.setBucket(tencentBucket);
        avatarConfiguration.setPrefix("/avatars");

        final COSClient client = new COSClient(new BasicCOSCredentials(tencentSecretId, tencentSecretKey), new ClientConfig(new Region(tencentRegion)));
        this.avatarService = new AvatarService(avatarConfiguration, client);
    }

    @Test
    @DisplayName("Should generate avatar URL with correct suffix")
    void testGenerateAvatarUrl() {
        final String s = avatarService.generateAvatarUrl("12345");
        assertTrue(s.endsWith("avatar-12345"));
    }

    @Test
    @DisplayName("Should generate avatar upload URL successfully when environment is configured")
    @EnabledIfEnvironmentVariable(named = "TENCENT_SECRET_KEY", matches = "^(?!.*key).*")
    void testGenerateAvatarUploadUrl_Success() throws IOException, IllegalContentTypeException {
        // Generate a simple image file
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        // Convert image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        final String principle = "12345";
        final String contentType = "image/png";
        final DataSize dataSize = DataSize.ofBytes(imageBytes.length);
        final String url = avatarService.generateAvatarUrl(principle);
        final String uploadUrl = avatarService.generateAvatarUploadUrl(principle, contentType, dataSize);
        assertTrue(uploadUrl.startsWith(url));

        // Create HTTP PUT request
        final HttpPut httpPut = new HttpPut(uploadUrl);
        httpPut.setHeader("Content-Type", contentType);

        // Set the request body
        HttpEntity entity = new ByteArrayEntity(imageBytes);
        httpPut.setEntity(entity);

        // Execute the request
        try (var client = HttpClients.createDefault()) {
            final var response = client.execute(httpPut);
            final int statusCode = response.getStatusLine().getStatusCode();
            assertEquals(200, statusCode);
        } catch (IOException e) {
            fail("Failed to upload the image: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw exception when content type is illegal")
    void testGenerateAvatarUploadUrl_IllegalContentType() {
        final String principle = "12345";
        final String contentType = "application/json";
        final DataSize dataSize = DataSize.ofBytes(1024);
        assertThrows(IllegalContentTypeException.class, () ->
                avatarService.generateAvatarUploadUrl(principle, contentType, dataSize)
        );
    }

    @Test
    @DisplayName("Should throw exception when file size exceeds maximum limit")
    void testGenerateAvatarUploadUrl_ExceedMaxSize() {
        final String principle = "12345";
        final String contentType = "image/png";
        final DataSize dataSize = DataSize.ofMegabytes(10); // Assuming max size is less than 5MB
        assertThrows(FileSizeLimitExceededException.class, () ->
                avatarService.generateAvatarUploadUrl(principle, contentType, dataSize)
        );
    }
}