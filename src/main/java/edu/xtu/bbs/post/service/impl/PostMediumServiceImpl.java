package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.config.MediaConfiguration;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostMediumServiceImpl implements PostMediumService {

    private final PostRepository postRepository;
    private final MediaConfiguration mediaConfiguration;
    private final MediaOssService mediaOssService;

    @Override
    public List<Medium> getMediaByPostId(Integer postId) {
        log.debug("Getting media for post {}", postId);
        
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return new ArrayList<>();
        }
        
        return post.getMedia() != null ? post.getMedia() : new ArrayList<>();
    }

    @Override
    public List<Medium> getMediaByPostId(Integer userId, Integer postId) {
        log.debug("Getting media for post {} with access control for user {}", postId, userId);
        
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return new ArrayList<>();
        }
        
        // Check access permission
        if (!hasAccessPermission(userId, post)) {
            log.warn("User {} does not have access to post {}", userId, postId);
            return new ArrayList<>();
        }
        
        return post.getMedia() != null ? post.getMedia() : new ArrayList<>();
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
        
        // Generate upload URL using OSS service
        String uploadUrl = mediaOssService.generateMediaUploadUrl(userId, uploadRequest.type(), uploadRequest.size());
        
        // Generate unique filename and access URL
        String filename = mediaOssService.generateUniqueFilename(uploadRequest.type());
        String accessUrl = mediaOssService.generateMediaAccessUrl(userId, filename);
        
        // Create medium record
        Medium medium = new Medium();
        medium.setId(generateMediumId());
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
        
        // Remove medium from list
        List<Medium> mediaList = post.getMedia() != null ? post.getMedia() : new ArrayList<>();
        boolean removed = mediaList.removeIf(medium -> medium.getId().equals(mediumId));
        
        if (!removed) {
            throw new DeletionFailedException(mediumId, "medium", "Medium not found in post");
        }
        
        post.setMedia(mediaList);
        postRepository.save(post);
        
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
        
        // Find post containing this medium
        List<Post> posts = postRepository.findAll();
        for (Post post : posts) {
            if (post.getMedia() != null) {
                for (Medium medium : post.getMedia()) {
                    if (medium.getId().equals(mediumId)) {
                        return hasAccessPermission(userId, post);
                    }
                }
            }
        }
        
        return false; // Medium not found or no access
    }

    private boolean hasAccessPermission(Integer userId, Post post) {
        // Owner always has access
        if (post.getAuthor().getId().equals(userId)) {
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
        
        // Check number of files
        int currentFileCount = post.getMedia() != null ? post.getMedia().size() : 0;
        if (currentFileCount >= mediaConfiguration.getMaxFiles()) {
            throw new TooManyMediaException(post.getId(), currentFileCount, 
                    mediaConfiguration.getMaxFiles());
        }
    }

    private String generateMediumId() {
        return UUID.randomUUID().toString();
    }
}