package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.MediaConfiguration;
import edu.xtu.bbs.post.config.PostMediaLimitConfiguration;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.dto.PostMediumUploadRequest;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.service.MediaOssService;
import edu.xtu.bbs.post.service.PostMediumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostMediumServiceImpl implements PostMediumService {

    private final PostRepository postRepository;
    private final MediaConfiguration mediaConfiguration;
    private final PostMediaLimitConfiguration mediaLimitConfiguration;
    private final MediaOssService mediaOssService;

    @Override
    public List<Medium> getMediaByPostId(Integer postId) {
        log.debug("Getting media for post {}", postId);

        return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .map(Post::getMedia)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    @Override
    public List<Medium> getMediaByPostId(Integer userId, Integer postId) {
        log.debug("Getting media for post {} with access control for user {}", postId, userId);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return new ArrayList<>();
        }

        if (!hasAccessPermission(userId, post)) {
            log.warn("User {} does not have access to post {}", userId, postId);
            return new ArrayList<>();
        }

        return post.getMedia() != null ? new ArrayList<>(post.getMedia()) : new ArrayList<>();
    }

    @Override
    @Transactional
    public MediumUploadResult uploadMediumToDraft(Integer userId, Integer postId, PostMediumUploadRequest uploadRequest)
            throws PostNotFoundException, UploadNotPermittedException, PostStatusNotAllowedException,
            UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException {
        
        log.debug("User {} uploading medium to post {}", userId, postId);
        
        // Validate post exists and is draft
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        
        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new UploadNotPermittedException(userId, postId, "User does not own post");
        }
        
        // Check status
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostStatusNotAllowedException(postId, post.getStatus(), "upload media");
        }
        
        // Validate upload request
        validateUploadRequest(uploadRequest, post);
        
        // Generate auto-increment ID for this post's media
        int autoIncrementId = getNextAutoIncrementId(post);
        
        // Generate upload URL using MediaOssService with postId and autoIncrementId
        String uploadUrl = mediaOssService.generateMediaUploadUrl(postId, autoIncrementId, uploadRequest.type(), uploadRequest.size());
        
        // Generate unique filename and access URL using postId and autoIncrementId
        String filename = mediaOssService.generateUniqueFilename(autoIncrementId, uploadRequest.type());
        String accessUrl = mediaOssService.generateMediaAccessUrl(postId, autoIncrementId, filename);
        
        // Create medium record
        Medium medium = new Medium();
        medium.setId(postId + "_" + autoIncrementId); // 使用 postId_autoIncrementId 格式
        medium.setType(uploadRequest.type());
        medium.setDisplayUrl(accessUrl);
        medium.setResourceUrl(accessUrl);
        
        // Add to post's media list
        List<Medium> mediaList = post.getMedia() != null ? post.getMedia() : new ArrayList<>();
        mediaList.add(medium);
        post.setMedia(mediaList);
        
        postRepository.save(post);
        
        log.info("Medium uploaded to post {} by user {}: {}", postId, userId, medium.getId());
        
        return new MediumUploadResult(uploadUrl, accessUrl);
    }

    @Override
    @Transactional
    public List<Medium> deleteMediumFromDraft(Integer userId, Integer postId, String mediumId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, DeletionFailedException {
        
        log.debug("User {} deleting medium {} from post {}", userId, mediumId, postId);
        
        // Validate post exists and is draft
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        
        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ModifyNotPermittedException(postId, userId, "User does not own post");
        }
        
        // Check status
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostStatusNotAllowedException(postId, post.getStatus(), "delete media");
        }
        
        // Get medium list
        List<Medium> mediaList = post.getMedia() != null ? post.getMedia() : new ArrayList<>();
        
        // Find the medium to get file info before deletion
        Medium deletedMedium = mediaList.stream()
                .filter(medium -> medium.getId().equals(mediumId))
                .findFirst()
                .orElse(null);
        
        // Remove medium from list
        boolean removed = mediaList.removeIf(medium -> medium.getId().equals(mediumId));

        if (!removed) {
            throw new DeletionFailedException(mediumId, "medium", "Medium not found in post");
        }

        post.setMedia(mediaList);
        postRepository.save(post);
        
        // Delete actual media file from cloud storage
        if (deletedMedium != null) {
            try {
                // Extract auto increment ID from medium ID or URL
                String filename = extractFilenameFromMedium(deletedMedium);
                Integer autoIncrementId = extractAutoIncrementIdFromMedium(deletedMedium);
                
                if (filename != null && autoIncrementId != null) {
                    boolean deleted = mediaOssService.deleteMediaFile(postId, autoIncrementId, filename);
                    if (deleted) {
                        log.info("Successfully deleted media file from cloud storage for medium {}", mediumId);
                    } else {
                        log.warn("Failed to delete media file from cloud storage for medium {}", mediumId);
                    }
                }
            } catch (Exception e) {
                log.error("Error deleting media file from cloud storage for medium {}: {}", mediumId, e.getMessage(), e);
            }
        }

        log.info("Medium {} deleted from post {} by user {}", mediumId, postId, userId);
        return mediaList;
    }

    @Override
    @Transactional
    public Medium updateMediumMetadata(Integer userId, Integer postId, String mediumId, String newType)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, UnsupportedMediumTypeException {
        
        log.debug("User {} updating medium {} metadata in post {}", userId, mediumId, postId);
        
        // Validate post exists and is draft
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        
        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ModifyNotPermittedException(postId, userId, "User does not own post");
        }
        
        // Check status
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostStatusNotAllowedException(postId, post.getStatus(), "update media metadata");
        }
        
        // Validate new type
        if (!mediaConfiguration.isTypeAllowed(newType)) {
            throw new UnsupportedMediumTypeException("medium", newType, 
                    mediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
        
        // Find and update medium
        List<Medium> mediaList = post.getMedia() != null ? post.getMedia() : new ArrayList<>();
        Medium targetMedium = null;
        
        for (Medium medium : mediaList) {
            if (medium.getId().equals(mediumId)) {
                medium.setType(newType);
                targetMedium = medium;
                break;
            }
        }
        
        if (targetMedium == null) {
            throw new PostNotFoundException("Medium not found with id: " + mediumId);
        }
        
        postRepository.save(post);
        
        log.info("Medium {} metadata updated in post {} by user {}", mediumId, postId, userId);
        
        return targetMedium;
    }

    @Override
    public Boolean hasAccessPermission(Integer userId, String mediumId) {
        log.debug("Checking access permission for user {} to medium {}", userId, mediumId);

        Integer postId = extractPostIdFromMediumId(mediumId);
        if (postId == null) {
            log.warn("Cannot derive post id from medium {}", mediumId);
            return false;
        }

        return postRepository.findById(postId)
                .filter(post -> post.getMedia() != null && post.getMedia().stream()
                        .anyMatch(medium -> mediumId.equals(medium.getId())))
                .map(post -> hasAccessPermission(userId, post))
                .orElse(false);
    }

    private boolean hasAccessPermission(Integer userId, Post post) {
        // Owner always has access
        if (post.getAuthor() != null && post.getAuthor().getId() != null && post.getAuthor().getId().equals(userId)) {
            return true;
        }
        
        // Published posts are publicly accessible
        return post.getStatus() == PostStatus.PUBLISHED;
    }

    private void validateUploadRequest(PostMediumUploadRequest request, Post post)
            throws UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException {
        
        // Check file type
        if (!mediaConfiguration.isTypeAllowed(request.type())) {
            throw new UnsupportedMediumTypeException("uploaded_file", request.type(), 
                    mediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
        
        // Check file size
        if (request.size() > mediaConfiguration.getMaxSizeBytes()) {
            throw new MediumSizeExceededException("uploaded_file", request.size(), 
                    mediaConfiguration.getMaxSizeBytes());
        }
        
        // Check number of files against configured limit
        int currentFileCount = post.getMedia() != null ? post.getMedia().size() : 0;
        int maxAllowedFiles = Math.min(mediaConfiguration.getMaxFiles(), mediaLimitConfiguration.getMaxPostMedia());
        if (currentFileCount >= maxAllowedFiles) {
            throw new TooManyMediaException(post.getId(), currentFileCount,
                maxAllowedFiles);
        }
    }

    /**
     * 获取帖子媒体文件的下一个自增ID
     * 
     * @param post 帖子对象
     * @return 下一个自增ID
     */
    private int getNextAutoIncrementId(Post post) {
        List<Medium> currentMedia = post.getMedia();
        if (currentMedia == null || currentMedia.isEmpty()) {
            return 1;
        }
        
        // 找到当前最大的自增ID
        int maxId = 0;
        for (Medium medium : currentMedia) {
            String mediumId = medium.getId();
            // 尝试解析ID中的数字部分
            try {
                // 假设ID格式为 postId_autoIncrementId 或直接是数字
                String[] parts = mediumId.split("_");
                if (parts.length >= 2) {
                    int currentId = Integer.parseInt(parts[parts.length - 1]);
                    maxId = Math.max(maxId, currentId);
                } else {
                    // 如果是纯数字ID
                    int currentId = Integer.parseInt(mediumId);
                    maxId = Math.max(maxId, currentId);
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，继续处理下一个
                log.debug("Cannot parse medium ID as integer: {}", mediumId);
            }
        }
        
        return maxId + 1;
    }
    
    /**
     * Extract filename from medium object based on its resource URL or ID
     */
    private String extractFilenameFromMedium(Medium medium) {
        try {
            // Try to extract filename from resourceUrl
            if (medium.getResourceUrl() != null) {
                String url = medium.getResourceUrl();
                // Extract filename from URL (after last slash)
                int lastSlash = url.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < url.length() - 1) {
                    String filename = url.substring(lastSlash + 1);
                    // Remove URL parameters if any
                    int paramIndex = filename.indexOf('?');
                    if (paramIndex != -1) {
                        filename = filename.substring(0, paramIndex);
                    }
                    return filename;
                }
            }
            
            // Fallback: use medium ID as filename if it contains extension
            if (medium.getId() != null && medium.getId().contains(".")) {
                return medium.getId();
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting filename from medium: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract auto increment ID from medium object
     */
    private Integer extractAutoIncrementIdFromMedium(Medium medium) {
        try {
            // Try to extract auto increment ID from filename or ID
            String identifier = null;
            
            if (medium.getResourceUrl() != null) {
                String url = medium.getResourceUrl();
                int lastSlash = url.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < url.length() - 1) {
                    identifier = url.substring(lastSlash + 1);
                    int paramIndex = identifier.indexOf('?');
                    if (paramIndex != -1) {
                        identifier = identifier.substring(0, paramIndex);
                    }
                }
            }
            
            if (identifier == null) {
                identifier = medium.getId();
            }
            
            if (identifier != null) {
                // Extract number before underscore (format: autoIncrementId_originalFilename)
                int underscoreIndex = identifier.indexOf('_');
                if (underscoreIndex > 0) {
                    String idPart = identifier.substring(0, underscoreIndex);
                    return Integer.valueOf(idPart);
                }
                
                // Try to extract number from start of identifier
                StringBuilder numberPart = new StringBuilder();
                for (char c : identifier.toCharArray()) {
                    if (Character.isDigit(c)) {
                        numberPart.append(c);
                    } else {
                        break;
                    }
                }
                
                if (numberPart.length() > 0) {
                    return Integer.valueOf(numberPart.toString());
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting auto increment ID from medium: {}", e.getMessage(), e);
            return null;
        }
    }

    private Integer extractPostIdFromMediumId(String mediumId) {
        if (mediumId == null || mediumId.isBlank()) {
            return null;
        }

        String candidate = mediumId;
        int underscoreIndex = mediumId.indexOf('_');
        if (underscoreIndex > 0) {
            candidate = mediumId.substring(0, underscoreIndex);
        }

        try {
            return Integer.valueOf(candidate);
        } catch (NumberFormatException ex) {
            log.warn("Medium id {} does not contain a valid post id", mediumId);
            return null;
        }
    }
}