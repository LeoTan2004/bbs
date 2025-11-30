package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.CommentMediaConfiguration;
import edu.xtu.bbs.post.config.PostMediaLimitConfiguration;
import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentMediaOssService;
import edu.xtu.bbs.post.service.PostCommentMediumService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostCommentMediumServiceImpl implements PostCommentMediumService {

    private final PostCommentRepository commentRepository;
    private final CommentMediaConfiguration commentMediaConfiguration;
    private final PostMediaLimitConfiguration mediaLimitConfiguration;
    private final CommentMediaOssService commentMediaOssService;


    @Override
    public List<Medium> getMediaByCommentId(@NotNull Integer commentId) {
        log.debug("Getting media for comment {}", commentId);
        
        Optional<PostComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Medium> media = commentOpt.get().getMedia();
        return media != null ? media : Collections.emptyList();
    }

    @Override
    public List<Medium> getMediaByCommentId(@NotNull Integer userId, @NotNull Integer commentId) {
        log.debug("Getting media for comment {} for user {}", commentId, userId);

        return getMediaByCommentId(commentId);
    }

    @Override
    @Transactional
    public MediumUploadResult uploadMediumToComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull CommentMediumUploadRequest uploadRequest)
            throws CommentNotFoundException, UploadNotPermittedException, UnsupportedMediumTypeException,
            MediumSizeExceededException, TooManyMediaException {
        
        log.debug("User {} uploading medium to comment {}", userId, commentId);
        
        // Verify comment exists
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        // Check if user has permission to upload (only comment owner can upload)
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new UploadNotPermittedException(userId, commentId, "User does not have permission to upload to this comment");
        }
        
        // Validate upload request
        validateUploadRequest(uploadRequest, comment);
        
        // Generate auto-increment ID for this comment's media
        int autoIncrementId = getNextAutoIncrementId(comment);
        
        // Generate upload URL using CommentMediaOssService with commentId and autoIncrementId
        String uploadUrl = commentMediaOssService.generateCommentMediaUploadUrl(commentId, autoIncrementId, uploadRequest.type(), uploadRequest.size());
        
        // Generate unique filename and access URL using commentId and autoIncrementId
        String filename = commentMediaOssService.generateUniqueFilename(autoIncrementId, uploadRequest.type());
        String accessUrl = commentMediaOssService.generateCommentMediaAccessUrl(commentId, autoIncrementId, filename);
        
        // Create medium record
        Medium medium = new Medium();
        medium.setId(commentId + "_" + autoIncrementId); // Use commentId_autoIncrementId format
        medium.setType(uploadRequest.type());
        medium.setDisplayUrl(accessUrl);
        medium.setResourceUrl(accessUrl);
        
        // Add to comment's media list
        List<Medium> mediaList = comment.getMedia() != null ? comment.getMedia() : new ArrayList<>();
        mediaList.add(medium);
        comment.setMedia(mediaList);
        
        commentRepository.save(comment);
        
        log.info("Medium uploaded to comment {} by user {}: {}", commentId, userId, medium.getId());
        
        return new MediumUploadResult(uploadUrl, accessUrl);
    }

    @Override
    @Transactional
    public List<Medium> deleteMediumFromComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull String mediumId)
            throws CommentNotFoundException, ModifyNotPermittedException, DeletionFailedException {
        
        log.debug("User {} deleting medium {} from comment {}", userId, mediumId, commentId);
        
        // Verify comment exists
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        // Check if user has permission to modify (only comment owner can modify)
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new ModifyNotPermittedException(userId, commentId, "User does not have permission to modify this comment");
        }
        
        // Get current media list
        List<Medium> currentMedia = comment.getMedia();
        if (currentMedia == null || currentMedia.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find the medium to get file info before deletion
        Medium deletedMedium = currentMedia.stream()
                .filter(medium -> Objects.equals(medium.getId(), mediumId))
                .findFirst()
                .orElse(null);
        
        // Remove the specified medium
        List<Medium> updatedMedia = currentMedia.stream()
                .filter(medium -> !Objects.equals(medium.getId(), mediumId))
                .collect(Collectors.toList());
        
        // Update comment with new media list
        comment.setMedia(updatedMedia);
        commentRepository.save(comment);
        
        // Delete actual media file from cloud storage
        if (deletedMedium != null) {
            try {
                // Extract auto increment ID from medium ID or URL
                String filename = extractFilenameFromMedium(deletedMedium);
                Integer autoIncrementId = extractAutoIncrementIdFromMedium(deletedMedium);
                
                if (filename != null && autoIncrementId != null) {
                    boolean deleted = commentMediaOssService.deleteCommentMediaFile(commentId, autoIncrementId, filename);
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
        
        log.info("Deleted medium {} from comment {}", mediumId, commentId);
        
        return updatedMedia;
    }

    @Transactional
    public Medium updateMediumMetadata(@NotNull Integer userId, @NotNull Integer commentId, @NotNull String mediumId, @NotNull String newType)
            throws CommentNotFoundException, ModifyNotPermittedException, UnsupportedMediumTypeException {
        
        log.debug("User {} updating medium {} metadata in comment {}", userId, mediumId, commentId);
        
        // Verify comment exists
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        // Check if user has permission to modify (only comment owner can modify)
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new ModifyNotPermittedException(userId, commentId, "User does not have permission to modify this comment");
        }
        
        // Validate new media type
        if (!commentMediaConfiguration.isTypeAllowed(newType)) {
            throw new UnsupportedMediumTypeException("comment_media", newType, 
                    commentMediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
        
        // Find and update the medium
        List<Medium> currentMedia = comment.getMedia();
        if (currentMedia == null) {
            throw new RuntimeException("Medium not found with id: " + mediumId);
        }
        
        Medium updatedMedium = null;
        for (Medium medium : currentMedia) {
            if (Objects.equals(medium.getId(), mediumId)) {
                medium.setType(newType);
                updatedMedium = medium;
                break;
            }
        }
        
        if (updatedMedium == null) {
            throw new RuntimeException("Medium not found with id: " + mediumId);
        }
        
        // Save the comment with updated media
        commentRepository.save(comment);
        
        log.info("Updated medium {} type to {} in comment {}", mediumId, newType, commentId);
        
        return updatedMedium;
    }

    public Boolean hasAccessPermission(@NotNull Integer userId, @NotNull String mediumId) {
        log.debug("Checking access permission for user {} on medium {}", userId, mediumId);
        
        try {
            // Find the comment that contains this medium
            List<PostComment> comments = commentRepository.findAll();
            PostComment owningComment = null;
            
            for (PostComment comment : comments) {
                if (comment.getMedia() != null) {
                    boolean hasMedium = comment.getMedia().stream()
                            .anyMatch(medium -> Objects.equals(medium.getId(), mediumId));
                    if (hasMedium) {
                        owningComment = comment;
                        break;
                    }
                }
            }
            
            if (owningComment == null) {
                log.debug("Medium {} not found in any comment", mediumId);
                return false;
            }
            
            // Check if comment and its post are accessible to user
            // Check post accessibility (assuming public access for published posts)
            Post post = owningComment.getPost();
            if (post.getStatus() == PostStatus.PUBLISHED) {
                return true;  // Published post comments are publicly accessible
            } else if (post.getStatus() == PostStatus.DRAFT) {
                // Draft posts are only accessible to the author
                return Objects.equals(post.getAuthor().getId(), userId);
            }
            
            // For other statuses, deny access
            return false;
            
        } catch (Exception e) {
            log.error("Error checking access permission for medium {}: {}", mediumId, e.getMessage(), e);
            return false;
        }
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

    private void validateUploadRequest(CommentMediumUploadRequest request, PostComment comment)
            throws UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException {
        
        // Check file type
        if (!commentMediaConfiguration.isTypeAllowed(request.type())) {
            throw new UnsupportedMediumTypeException("comment_media_file", request.type(),
                    commentMediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
        
        // Check file size
        if (request.size() > commentMediaConfiguration.getMaxSizeBytes()) {
            throw new MediumSizeExceededException("comment_media", request.size(), 
                    commentMediaConfiguration.getMaxSizeBytes());
        }
        
        // Check number of files against configured limit
        int currentFileCount = comment.getMedia() != null ? comment.getMedia().size() : 0;
        int maxAllowedFiles = Math.min(commentMediaConfiguration.getMaxFiles(), mediaLimitConfiguration.getMaxCommentMedia());
        if (currentFileCount >= maxAllowedFiles) {
            throw new TooManyMediaException(comment.getId(), currentFileCount,
                maxAllowedFiles);
        }
    }

    private String generateMediumId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get the next auto-increment ID for comment media files
     *
     * @param comment comment object
     * @return next auto-increment ID
     */
    private int getNextAutoIncrementId(PostComment comment) {
        List<Medium> currentMedia = comment.getMedia();
        if (currentMedia == null || currentMedia.isEmpty()) {
            return 1;
        }
        
        // Find the current maximum auto-increment ID
        int maxId = 0;
        for (Medium medium : currentMedia) {
            String mediumId = medium.getId();
            // Try to parse the numeric part from the ID
            try {
                // Assume ID format is commentId_autoIncrementId or direct number
                String[] parts = mediumId.split("_");
                if (parts.length >= 2) {
                    int currentId = Integer.parseInt(parts[parts.length - 1]);
                    maxId = Math.max(maxId, currentId);
                } else {
                    // If it's a pure numeric ID
                    int currentId = Integer.parseInt(mediumId);
                    maxId = Math.max(maxId, currentId);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, continue with next one
                log.debug("Cannot parse medium ID as integer: {}", mediumId);
            }
        }
        
        return maxId + 1;
    }
}