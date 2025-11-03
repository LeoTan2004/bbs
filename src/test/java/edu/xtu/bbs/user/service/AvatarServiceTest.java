package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.exception.IllegalContentTypeException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "oss.secret-id=${TENCENT_SECRET_ID:id}",
        "oss.secret-key=${TENCENT_SECRET_KEY:key}",
        "oss.region=${TENCENT_REGION:ap-guangzhou}",
        "oss.avatar.bucket=${TENCENT_BUCKET:avatar-bucket}",
})
class AvatarServiceTest {

    @Autowired
    private AvatarService avatarService;

    @Test
    void testGenerateAvatarUrl() {
        final String s = avatarService.generateAvatarUrl("12345");
        assertTrue(s.endsWith("avatar-12345"));
    }

    @Test
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
    void testGenerateAvatarUploadUrl_IllegalContentType() {
        final String principle = "12345";
        final String contentType = "application/json";
        final DataSize dataSize = DataSize.ofBytes(1024);
        assertThrows(IllegalContentTypeException.class, () ->
                avatarService.generateAvatarUploadUrl(principle, contentType, dataSize)
        );
    }

    @Test
    void testGenerateAvatarUploadUrl_ExceedMaxSize() {
        final String principle = "12345";
        final String contentType = "image/png";
        final DataSize dataSize = DataSize.ofMegabytes(10); // Assuming max size is less than 5MB
        assertThrows(org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException.class, () ->
                avatarService.generateAvatarUploadUrl(principle, contentType, dataSize)
        );
    }
}