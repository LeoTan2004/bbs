package edu.xtu.bbs.post.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import edu.xtu.bbs.post.config.MediaConfiguration;
import edu.xtu.bbs.post.exception.UnsupportedMediumTypeException;
import edu.xtu.bbs.post.exception.MediumSizeExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Media file cloud storage service
 * Handles cloud storage operations for media files, including generating upload URLs, access URLs, etc.
 * 
 * @author BBS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaOssService {

    private final MediaConfiguration mediaConfiguration;
    private final COSClient cosClient;

    /**
     * Generate unique key for media file in cloud storage
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param filename File name
     * @return Storage key
     */
    private String generateMediaKey(Integer postId, Integer autoIncrementId, String filename) {
        return "%s/%d/%d_%s".formatted(
                mediaConfiguration.getPrefix(), 
                postId, 
                autoIncrementId,
                filename
        );
    }

    /**
     * Generate access URL for media file
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param filename File name
     * @return Access URL
     */
    public String generateMediaAccessUrl(Integer postId, Integer autoIncrementId, String filename) {
        final String objectKey = generateMediaKey(postId, autoIncrementId, filename);
        return cosClient.getObjectUrl(mediaConfiguration.getBucket(), objectKey).toString();
    }

    /**
     * Check if file type is allowed for upload
     * 
     * @param contentType File MIME type
     * @throws UnsupportedMediumTypeException Unsupported file type
     */
    private void checkAllowType(String contentType) throws UnsupportedMediumTypeException {
        final boolean contains = mediaConfiguration.getAllowTypes().contains(contentType);
        if (!contains) {
            throw new UnsupportedMediumTypeException("media_file", contentType, 
                    mediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
    }

    /**
     * Check if file size exceeds the limit
     * 
     * @param dataSize File size
     * @throws MediumSizeExceededException File size exceeds limit
     */
    private void checkDataSize(DataSize dataSize) throws MediumSizeExceededException {
        final long maxSize = mediaConfiguration.getMaxSize().toBytes();
        if (dataSize.toBytes() > maxSize) {
            throw new MediumSizeExceededException("media_file", dataSize.toBytes(), maxSize);
        }
    }

    /**
     * Generate unique filename for media file
     * 
     * @param autoIncrementId Auto increment ID
     * @param contentType File MIME type
     * @return Unique filename
     */
    public String generateUniqueFilename(Integer autoIncrementId, String contentType) {
        String extension = getFileExtension(contentType);
        return autoIncrementId + extension;
    }

    /**
     * Get file extension based on MIME type
     * 
     * @param contentType MIME type
     * @return File extension
     */
    private String getFileExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "video/mp4" -> ".mp4";
            case "video/mpeg" -> ".mpeg";
            case "video/quicktime" -> ".mov";
            case "video/webm" -> ".webm";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> "";
        };
    }

    /**
     * Generate upload URL for media file
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param contentType File MIME type
     * @param dataSize File size
     * @return Upload URL
     * @throws UnsupportedMediumTypeException Unsupported file type
     * @throws MediumSizeExceededException File size exceeds limit
     */
    public String generateMediaUploadUrl(Integer postId, Integer autoIncrementId, String contentType, DataSize dataSize) 
            throws UnsupportedMediumTypeException, MediumSizeExceededException {
        
        checkAllowType(contentType);
        checkDataSize(dataSize);
        
        final String filename = generateUniqueFilename(autoIncrementId, contentType);
        final String objectKey = generateMediaKey(postId, autoIncrementId, filename);
        final Instant expiredAt = Instant.now().plus(mediaConfiguration.getExpiredAfter());
        final Date expireDate = Date.from(expiredAt);
        final Map<String, String> headers = Map.of(
                "Content-Type", contentType, 
                "Content-Length", String.valueOf(dataSize.toBytes())
        );
        
        final String bucket = mediaConfiguration.getBucket();
        final URL url = cosClient.generatePresignedUrl(bucket, objectKey, expireDate, 
                HttpMethodName.PUT, headers, Map.of());
        
        log.info("Generated upload URL for post media - post {} with content type {} and size {}", 
                postId, contentType, dataSize);
        
        return url.toString();
    }

    /**
     * Generate upload URL for media file (overloaded method)
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param contentType File MIME type
     * @param sizeInBytes File size in bytes
     * @return Upload URL
     * @throws UnsupportedMediumTypeException Unsupported file type
     * @throws MediumSizeExceededException File size exceeds limit
     */
    public String generateMediaUploadUrl(Integer postId, Integer autoIncrementId, String contentType, long sizeInBytes) 
            throws UnsupportedMediumTypeException, MediumSizeExceededException {
        return generateMediaUploadUrl(postId, autoIncrementId, contentType, DataSize.ofBytes(sizeInBytes));
    }

    /**
     * Delete media file from cloud storage
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param filename File name
     * @return Whether deletion was successful
     */
    public boolean deleteMediaFile(Integer postId, Integer autoIncrementId, String filename) {
        try {
            final String objectKey = generateMediaKey(postId, autoIncrementId, filename);
            cosClient.deleteObject(mediaConfiguration.getBucket(), objectKey);
            log.info("Successfully deleted media file for post {}: {}", postId, filename);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete media file for post {}: {}", postId, filename, e);
            return false;
        }
    }

    /**
     * Check if media file exists in cloud storage
     * 
     * @param postId Post ID
     * @param autoIncrementId Auto increment ID for this post's media
     * @param filename File name
     * @return Whether file exists
     */
    public boolean mediaFileExists(Integer postId, Integer autoIncrementId, String filename) {
        try {
            final String objectKey = generateMediaKey(postId, autoIncrementId, filename);
            return cosClient.doesObjectExist(mediaConfiguration.getBucket(), objectKey);
        } catch (Exception e) {
            log.error("Failed to check media file existence for post {}: {}", postId, filename, e);
            return false;
        }
    }
}