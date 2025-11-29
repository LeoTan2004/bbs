package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.CommentMediaConfiguration;
import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.model.PostComment;
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
        
        // Generate upload URL using CommentMediaOssService
        String uploadUrl = commentMediaOssService.generateCommentMediaUploadUrl(userId, uploadRequest.type(), uploadRequest.size());
        
        // Generate unique filename and access URL
        String filename = commentMediaOssService.generateUniqueFilename(uploadRequest.type());
        String accessUrl = commentMediaOssService.generateCommentMediaAccessUrl(userId, filename);
        
        // Create medium record
        Medium medium = new Medium();
        medium.setId(generateMediumId());
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

    @Override
    public Boolean hasAccessPermission(@NotNull Integer userId, @NotNull String mediumId) {
        log.debug("Checking access permission for user {} on medium {}", userId, mediumId);
        
        // TODO: Implement actual permission check logic
        // For now, assume all comment media is publicly accessible
        return true;
    }

    private void validateUploadRequest(CommentMediumUploadRequest request, PostComment comment)
            throws UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException {
        
        // Check file type
        if (!commentMediaConfiguration.isTypeAllowed(request.type())) {
            throw new UnsupportedMediumTypeException("comment_media", request.type(), 
                    commentMediaConfiguration.getAllowTypes().toArray(new String[0]));
        }
        
        // Check file size
        if (request.size() > commentMediaConfiguration.getMaxSizeBytes()) {
            throw new MediumSizeExceededException("comment_media", request.size(), 
                    commentMediaConfiguration.getMaxSizeBytes());
        }
        
        // Check number of files
        int currentFileCount = comment.getMedia() != null ? comment.getMedia().size() : 0;
        if (currentFileCount >= commentMediaConfiguration.getMaxFiles()) {
            throw new TooManyMediaException(comment.getId(), currentFileCount, 
                    commentMediaConfiguration.getMaxFiles());
        }
    }

    private String generateMediumId() {
        return UUID.randomUUID().toString();
    }

}