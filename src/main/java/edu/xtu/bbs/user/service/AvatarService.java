package edu.xtu.bbs.user.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import edu.xtu.bbs.user.config.AvatarConfiguration;
import edu.xtu.bbs.user.exception.IllegalContentTypeException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class AvatarService {

    private final AvatarConfiguration avatarConfiguration;
    private final COSClient cosClient;

    public AvatarService(AvatarConfiguration avatarConfiguration, COSClient cosClient) {
        this.avatarConfiguration = avatarConfiguration;
        this.cosClient = cosClient;
    }

    private String generateAvatarKey(String principle) {
        return "%s/avatar-%s".formatted(avatarConfiguration.getPrefix(), principle);
    }

    public String generateAvatarUrl(String principle) {
        final String objectKey = generateAvatarKey(principle);
        return cosClient.getObjectUrl(avatarConfiguration.getBucket(), objectKey).toString();
    }

    private void checkAllowType(String type) throws IllegalContentTypeException {
        final boolean contains = avatarConfiguration.getAllowTypes().contains(type);
        if (!contains) {
            throw new IllegalContentTypeException(type, "Avatar not allow this type");
        }
    }

    private void checkDataSize(DataSize dataSize) throws FileSizeLimitExceededException {
        final long maxSize = avatarConfiguration.getMaxSize().toBytes();
        if (dataSize.toBytes() > maxSize) {
            throw new FileSizeLimitExceededException("Avatar size exceed max size", dataSize.toBytes(), maxSize);
        }
    }


    public String generateAvatarUploadUrl(String principle, String contentType, DataSize dataSize) throws IllegalContentTypeException, FileSizeLimitExceededException {
        checkAllowType(contentType);
        checkDataSize(dataSize);
        final String objectKey = generateAvatarKey(principle);
        final Instant expiredAt = Instant.now().plus(avatarConfiguration.getExpiredAfter());
        final Date expireDate = Date.from(expiredAt);
        final Map<String, String> header = Map.of("Content-Type", contentType, "Content-Length", String.valueOf(dataSize.toBytes()));
        final String bucket = avatarConfiguration.getBucket();
        final URL url = cosClient.generatePresignedUrl(bucket, objectKey, expireDate, HttpMethodName.PUT, header, Map.of());
        return url.toString();
    }
}
