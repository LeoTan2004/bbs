package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.MediaOssService;
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
    private final MediaOssService mediaOssService;

    // Maximum number of media files per comment
    private static final int MAX_MEDIA_PER_COMMENT = 9;
    
    // Maximum file size (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    // Supported media types
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/avi",
            "audio/mp3", "audio/wav", "audio/ogg"
    );

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
        
        // For comments, media is usually public unless the comment is deleted
        // TODO: Add access control logic if needed
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
        
        // Validate file type
        if (!SUPPORTED_MEDIA_TYPES.contains(uploadRequest.type())) {
            throw new UnsupportedMediumTypeException("comment_media", uploadRequest.type(), SUPPORTED_MEDIA_TYPES.toArray(new String[0]));
        }
        
        // Validate file size
        if (uploadRequest.size() > MAX_FILE_SIZE) {
            throw new MediumSizeExceededException("comment_media", uploadRequest.size(), MAX_FILE_SIZE);
        }
        
        // Check media count limit
        List<Medium> existingMedia = comment.getMedia();
        if (existingMedia != null && existingMedia.size() >= MAX_MEDIA_PER_COMMENT) {
            throw new TooManyMediaException(commentId, existingMedia.size(), MAX_MEDIA_PER_COMMENT);
        }
        
        // Generate upload URL using MediaOssService
        // TODO: Implement actual media upload logic
        String mediumId = UUID.randomUUID().toString();
        String uploadUrl = "https://upload.example.com/" + mediumId;
        String accessUrl = "https://media.example.com/" + mediumId;
        
        log.info("Generated upload URLs for medium {} in comment {}", mediumId, commentId);
        
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
        
        // Remove the specified medium
        List<Medium> updatedMedia = currentMedia.stream()
                .filter(medium -> !Objects.equals(medium.getId(), mediumId))
                .collect(Collectors.toList());
        
        // Update comment with new media list
        comment.setMedia(updatedMedia);
        commentRepository.save(comment);
        
        // TODO: Delete actual media file from storage
        // mediaOssService.deleteMedia(mediumId);
        
        log.info("Deleted medium {} from comment {}", mediumId, commentId);
        
        return updatedMedia;
    }

    @Override
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
        if (!SUPPORTED_MEDIA_TYPES.contains(newType)) {
            throw new UnsupportedMediumTypeException("comment_media", newType, SUPPORTED_MEDIA_TYPES.toArray(new String[0]));
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

    @Override
    public Boolean hasAccessPermission(@NotNull Integer userId, @NotNull String mediumId) {
        log.debug("Checking access permission for user {} on medium {}", userId, mediumId);
        
        // TODO: Implement actual permission check logic
        // For now, assume all comment media is publicly accessible
        return true;
    }

}