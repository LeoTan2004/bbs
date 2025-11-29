package edu.xtu.bbs.post.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import edu.xtu.bbs.post.config.CommentMediaConfiguration;
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
 * Comment media file cloud storage service
 * Handles cloud storage operations for comment media files, including generating upload URLs, access URLs, etc.
 * 
 * @author BBS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentMediaOssService {

    private final CommentMediaConfiguration commentMediaConfiguration;
    private final COSClient cosClient;

    /**
     * Generate unique key for comment media file in cloud storage
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param filename File name
     * @return Storage key
     */
    private String generateCommentMediaKey(Integer commentId, Integer autoIncrementId, String filename) {
        return "%s/%d/%d_%s".formatted(
                commentMediaConfiguration.getPrefix(), 
                commentId, 
                autoIncrementId,
                filename
        );
    }

    /**
     * Generate access URL for comment media file
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param filename File name
     * @return Access URL
     */
    public String generateCommentMediaAccessUrl(Integer commentId, Integer autoIncrementId, String filename) {
        final String objectKey = generateCommentMediaKey(commentId, autoIncrementId, filename);
        return cosClient.getObjectUrl(commentMediaConfiguration.getBucket(), objectKey).toString();
    }

    /**
     * Check if file type is allowed for upload
     * 
     * @param contentType File MIME type
     * @throws UnsupportedMediumTypeException Unsupported file type
     */
    private void checkAllowType(String contentType) throws UnsupportedMediumTypeException {
        final boolean contains = commentMediaConfiguration.getAllowTypes().contains(contentType);
        if (!contains) {
            throw new UnsupportedMediumTypeException("comment_media_file", contentType, 
                    commentMediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
    }

    /**
     * Check if file size exceeds the limit
     * 
     * @param dataSize File size
     * @throws MediumSizeExceededException File size exceeds limit
     */
    private void checkDataSize(DataSize dataSize) throws MediumSizeExceededException {
        final long maxSize = commentMediaConfiguration.getMaxSize().toBytes();
        if (dataSize.toBytes() > maxSize) {
            throw new MediumSizeExceededException("comment_media_file", dataSize.toBytes(), maxSize);
        }
    }

    /**
     * Generate unique filename for comment media file
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
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            default -> "";
        };
    }

    /**
     * Generate upload URL for comment media file
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param contentType File MIME type
     * @param dataSize File size
     * @return Upload URL
     * @throws UnsupportedMediumTypeException Unsupported file type
     * @throws MediumSizeExceededException File size exceeds limit
     */
    public String generateCommentMediaUploadUrl(Integer commentId, Integer autoIncrementId, String contentType, DataSize dataSize) 
            throws UnsupportedMediumTypeException, MediumSizeExceededException {
        
        checkAllowType(contentType);
        checkDataSize(dataSize);
        
        final String filename = generateUniqueFilename(autoIncrementId, contentType);
        final String objectKey = generateCommentMediaKey(commentId, autoIncrementId, filename);
        final Instant expiredAt = Instant.now().plus(commentMediaConfiguration.getExpiredAfter());
        final Date expireDate = Date.from(expiredAt);
        final Map<String, String> headers = Map.of(
                "Content-Type", contentType, 
                "Content-Length", String.valueOf(dataSize.toBytes())
        );
        
        final String bucket = commentMediaConfiguration.getBucket();
        final URL url = cosClient.generatePresignedUrl(bucket, objectKey, expireDate, 
                HttpMethodName.PUT, headers, Map.of());
        
        log.info("Generated upload URL for comment media - comment {} with content type {} and size {}", 
                commentId, contentType, dataSize);
        
        return url.toString();
    }

    /**
     * Generate upload URL for comment media file (overloaded method)
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param contentType File MIME type
     * @param sizeInBytes File size in bytes
     * @return Upload URL
     * @throws UnsupportedMediumTypeException Unsupported file type
     * @throws MediumSizeExceededException File size exceeds limit
     */
    public String generateCommentMediaUploadUrl(Integer commentId, Integer autoIncrementId, String contentType, long sizeInBytes) 
            throws UnsupportedMediumTypeException, MediumSizeExceededException {
        return generateCommentMediaUploadUrl(commentId, autoIncrementId, contentType, DataSize.ofBytes(sizeInBytes));
    }

    /**
     * Delete comment media file from cloud storage
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param filename File name
     * @return Whether deletion was successful
     */
    public boolean deleteCommentMediaFile(Integer commentId, Integer autoIncrementId, String filename) {
        try {
            final String objectKey = generateCommentMediaKey(commentId, autoIncrementId, filename);
            cosClient.deleteObject(commentMediaConfiguration.getBucket(), objectKey);
            log.info("Successfully deleted comment media file for comment {}: {}", commentId, filename);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete comment media file for comment {}: {}", commentId, filename, e);
            return false;
        }
    }

    /**
     * Check if comment media file exists in cloud storage
     * 
     * @param commentId Comment ID
     * @param autoIncrementId Auto increment ID for this comment's media
     * @param filename File name
     * @return Whether file exists
     */
    public boolean commentMediaFileExists(Integer commentId, Integer autoIncrementId, String filename) {
        try {
            final String objectKey = generateCommentMediaKey(commentId, autoIncrementId, filename);
            return cosClient.doesObjectExist(commentMediaConfiguration.getBucket(), objectKey);
        } catch (Exception e) {
            log.error("Failed to check comment media file existence for comment {}: {}", commentId, filename, e);
            return false;
        }
    }
}