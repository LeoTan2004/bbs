package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;
import edu.xtu.bbs.notification.service.NotificationService;
import edu.xtu.bbs.post.dto.CommentResponse;
import edu.xtu.bbs.post.dto.CreateCommentRequest;
import edu.xtu.bbs.post.dto.DraftCommentEditor;
import edu.xtu.bbs.post.dto.UpdateCommentRequest;
import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.exception.CommentPermissionDeniedException;
import edu.xtu.bbs.post.exception.CommentStatusNotAllowedException;
import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.CommentStatus;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.post.service.PostCommentService;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class PostCommentServiceImpl implements PostCommentService {

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PublicMatricRepository publicMatricRepository;
    private final CommentLikeService commentLikeService;
    private final NotificationService notificationService;

    private static final int NOTIFICATION_TITLE_LIMIT = 255;
    private static final int NOTIFICATION_SNIPPET_LIMIT = 512;
    private static final int CONTEXT_PREVIEW_LIMIT = 120;

    // region Comment CRUD Operations

    @Override
    @Transactional
    public PostComment createComment(@NotNull Integer userId, @NotNull CreateCommentRequest request) 
            throws PostNotFoundException, PostStatusNotAllowedException, CommentNotFoundException {
        
        log.debug("User {} creating comment for post {}", userId, request.postId());
        
        // Verify post exists and is published
        Post post = postRepository.findByIdAndStatus(request.postId(), PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    Optional<Post> existingPost = postRepository.findById(request.postId());
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + request.postId());
                    } else {
                        return new PostStatusNotAllowedException(request.postId(), 
                                existingPost.get().getStatus(), "comment", "Post is not published");
                    }
                });

        // Verify parent comment exists if specified
        PostComment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new CommentNotFoundException(request.parentCommentId()));
            // Verify parent comment belongs to the same post
            if (!Objects.equals(parentComment.getPost().getId(), request.postId())) {
                throw new CommentNotFoundException("Parent comment does not belong to the specified post");
            }
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Create comment
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setParentPostId(request.parentCommentId());
        comment.setContent(request.content());
        comment.setMedia(Collections.emptyList()); // Media managed separately
        comment.setStatus(CommentStatus.PUBLISHED); // Direct publish
        comment.setComments(0);
        comment.setLikes(0);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        comment = commentRepository.save(comment);

        // Update parent comment's reply count if it's a reply
        if (request.parentCommentId() != null) {
            updateCommentCount(request.parentCommentId());
        }

        // Update post's comment count
        updatePostCommentCount(request.postId());

        dispatchNotificationsForNewComment(user, post, comment, parentComment);

        log.info("User {} created comment {} for post {}", userId, comment.getId(), request.postId());
        
        return comment;
    }

    @Override
    @Transactional
    public PostComment updateComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull UpdateCommentRequest request) 
            throws CommentNotFoundException, CommentPermissionDeniedException {
        
        log.debug("User {} updating comment {}", userId, commentId);
        
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        // Check if user is the owner of the comment
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new CommentPermissionDeniedException(userId, commentId, "update");
        }

        // Update comment content only (media managed separately)
        comment.setContent(request.content());
        comment.setUpdatedAt(Instant.now());

        comment = commentRepository.save(comment);

        log.info("User {} updated comment {}", userId, commentId);
        
        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(@NotNull Integer userId, @NotNull Integer commentId) 
            throws CommentNotFoundException, CommentPermissionDeniedException {
        
        log.debug("User {} deleting comment {}", userId, commentId);
        
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        // Check if user is the owner of the comment
        if (!Objects.equals(comment.getUser().getId(), userId)) {
            throw new CommentPermissionDeniedException(userId, commentId, "delete");
        }

        Integer postId = comment.getPost().getId();
        Integer parentCommentId = comment.getParentPostId();

        commentRepository.delete(comment);

        // Update parent comment's reply count if it's a reply
        if (parentCommentId != null) {
            updateCommentCount(parentCommentId);
        }

        // Update post's comment count
        updatePostCommentCount(postId);

        log.info("User {} deleted comment {}", userId, commentId);
    }

    // endregion

    // region Comment Retrieval

    @Override
    public PostComment getCommentById(@NotNull Integer commentId) {
        log.debug("Getting comment {}", commentId);
        return commentRepository.findById(commentId).orElse(null);
    }

    @Override
    public CommentResponse getCommentResponseById(@NotNull Integer commentId, Integer currentUserId)
            throws CommentNotFoundException {
        
        log.debug("Getting comment {} for user {}", commentId, currentUserId);
        
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        return convertToCommentResponse(comment, currentUserId);
    }

    @Override
    public Page<PostComment> getCommentsByPost(@NotNull Integer postId, @NotNull Pageable pageable) 
            throws PostNotFoundException {
        
        log.debug("Getting comments for post {}", postId);
        
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException("Post not found with id: " + postId);
        }

        // Get top-level published comments (no parent comment) ordered by creation time
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return commentRepository.findByPostIdAndParentPostIdIsNullAndStatus(postId, CommentStatus.PUBLISHED, sortedPageable);
    }

    @Override
    public Page<CommentResponse> getCommentResponsesByPost(@NotNull Integer postId, Integer currentUserId, @NotNull Pageable pageable) 
            throws PostNotFoundException {
        
        log.debug("Getting comment responses for post {} for user {}", postId, currentUserId);
        
        Page<PostComment> comments = getCommentsByPost(postId, pageable);
        return comments.map(comment -> convertToCommentResponse(comment, currentUserId));
    }

    @Override
    public Page<PostComment> getRepliesByComment(@NotNull Integer parentCommentId, @NotNull Pageable pageable) 
            throws CommentNotFoundException {
        
        log.debug("Getting replies for comment {}", parentCommentId);
        
        // Verify parent comment exists
        if (!commentRepository.existsById(parentCommentId)) {
            throw new CommentNotFoundException(parentCommentId);
        }

        // Get published reply comments ordered by creation time
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        
        return commentRepository.findByParentPostIdAndStatus(parentCommentId, CommentStatus.PUBLISHED, sortedPageable);
    }

    @Override
    public Page<CommentResponse> getReplyResponsesByComment(@NotNull Integer parentCommentId, Integer currentUserId, @NotNull Pageable pageable) 
            throws CommentNotFoundException {
        
        log.debug("Getting reply responses for comment {} for user {}", parentCommentId, currentUserId);
        
        Page<PostComment> replies = getRepliesByComment(parentCommentId, pageable);
        return replies.map(comment -> convertToCommentResponse(comment, currentUserId));
    }

    @Override
    public Page<PostComment> getCommentsByUser(@NotNull Integer userId, @NotNull Pageable pageable) {
        
        log.debug("Getting comments by user {}", userId);
        
        // Get user's published comments ordered by creation time
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return commentRepository.findByUserIdAndStatus(userId, CommentStatus.PUBLISHED, sortedPageable);
    }

    @Override
    public Page<CommentResponse> getCommentResponsesByUser(@NotNull Integer userId, Integer currentUserId, @NotNull Pageable pageable) {
        
        log.debug("Getting comment responses by user {} for user {}", userId, currentUserId);
        
        Page<PostComment> comments = getCommentsByUser(userId, pageable);
        return comments.map(comment -> convertToCommentResponse(comment, currentUserId));
    }

    // endregion

    // region Statistics

    @Override
    public Integer countCommentsByPost(@NotNull Integer postId) {
        log.debug("Counting published comments for post {}", postId);
        return Math.toIntExact(commentRepository.countByPostIdAndStatus(postId, CommentStatus.PUBLISHED));
    }

    @Override
    public Integer countTopLevelCommentsByPost(@NotNull Integer postId) {
        log.debug("Counting published top-level comments for post {}", postId);
        return Math.toIntExact(commentRepository.countByPostIdAndParentPostIdIsNullAndStatus(postId, CommentStatus.PUBLISHED));
    }

    @Override
    public Integer countRepliesByComment(@NotNull Integer parentCommentId) {
        log.debug("Counting published replies for comment {}", parentCommentId);
        return Math.toIntExact(commentRepository.countByParentPostIdAndStatus(parentCommentId, CommentStatus.PUBLISHED));
    }

    @Override
    public Integer countCommentsByUser(@NotNull Integer userId) {
        log.debug("Counting published comments by user {}", userId);
        return Math.toIntExact(commentRepository.countByUserIdAndStatus(userId, CommentStatus.PUBLISHED));
    }

    // endregion

    // region Permission Check

    @Override
    public Boolean hasModifyPermission(@NotNull Integer userId, @NotNull Integer commentId) {
        log.debug("Checking modify permission for user {} on comment {}", userId, commentId);
        
        Optional<PostComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return false;
        }
        
        return Objects.equals(commentOpt.get().getUser().getId(), userId);
    }

    // endregion

    // region Private Helper Methods

    private void dispatchNotificationsForNewComment(User commenter, Post post, PostComment comment, PostComment parentComment) {
        if (post.getAuthor() != null && !Objects.equals(post.getAuthor().getId(), commenter.getId())) {
            Map<String, Object> context = buildCommentContext(post, comment, parentComment);
            notificationService.create(new NotificationCreateRequest(
                    post.getAuthor().getId(),
                    commenter.getId(),
                    NotificationType.POST_COMMENTED,
                    NotificationPriority.NORMAL,
                    truncate("你的帖子《" + post.getTitle() + "》收到新评论", NOTIFICATION_TITLE_LIMIT),
                    comment.getContent(),
                    "/posts/" + post.getId(),
                    "POST",
                    String.valueOf(post.getId()),
                    truncate(comment.getContent(), NOTIFICATION_SNIPPET_LIMIT),
                    context
            ));
        }

        if (parentComment != null && parentComment.getUser() != null
                && !Objects.equals(parentComment.getUser().getId(), commenter.getId())
                && !Objects.equals(parentComment.getUser().getId(), post.getAuthor() != null ? post.getAuthor().getId() : null)) {
            Map<String, Object> context = buildReplyContext(post, comment, parentComment);
            notificationService.create(new NotificationCreateRequest(
                    parentComment.getUser().getId(),
                    commenter.getId(),
                    NotificationType.COMMENT_REPLIED,
                    NotificationPriority.NORMAL,
                    truncate("你的评论收到新回复", NOTIFICATION_TITLE_LIMIT),
                    comment.getContent(),
                    "/posts/" + post.getId(),
                    "COMMENT",
                    String.valueOf(parentComment.getId()),
                    truncate(comment.getContent(), NOTIFICATION_SNIPPET_LIMIT),
                    context
            ));
        }
    }

    private Map<String, Object> buildCommentContext(Post post, PostComment comment, PostComment parentComment) {
        Map<String, Object> context = new HashMap<>();
        putIfNotNull(context, "postId", post.getId());
        putIfNotNull(context, "postTitle", truncate(post.getTitle(), CONTEXT_PREVIEW_LIMIT));
        putIfNotNull(context, "commentId", comment.getId());
        putIfNotNull(context, "commentPreview", truncate(comment.getContent(), CONTEXT_PREVIEW_LIMIT));
        if (parentComment != null) {
            putIfNotNull(context, "parentCommentId", parentComment.getId());
        }
        return context;
    }

    private Map<String, Object> buildReplyContext(Post post, PostComment comment, PostComment parentComment) {
        Map<String, Object> context = buildCommentContext(post, comment, parentComment);
        putIfNotNull(context, "parentCommentPreview", truncate(parentComment.getContent(), CONTEXT_PREVIEW_LIMIT));
        if (parentComment.getUser() != null) {
            putIfNotNull(context, "parentCommentAuthorId", parentComment.getUser().getId());
        }
        return context;
    }

    private void putIfNotNull(Map<String, Object> context, String key, Object value) {
        if (value != null) {
            context.put(key, value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private CommentResponse convertToCommentResponse(@NotNull PostComment comment, Integer currentUserId) {
        // Check if current user has liked this comment
        Boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = commentLikeService.hasLikedComment(currentUserId, comment.getId());
        }
        
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getAvatarUrl(),
                comment.getParentPostId(),
                comment.getContent(),
                comment.getMedia(),
                comment.getComments(),
                comment.getLikes(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isLiked
        );
    }

    private void updateCommentCount(@NotNull Integer commentId) {
        try {
            Optional<PostComment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isPresent()) {
                PostComment comment = commentOpt.get();
                Long replyCount = commentRepository.countByParentPostIdAndStatus(commentId, CommentStatus.PUBLISHED);
                comment.setComments(Math.toIntExact(replyCount));
                commentRepository.save(comment);
                
                log.debug("Updated comment count for comment {} to {}", commentId, replyCount);
            }
        } catch (Exception e) {
            log.warn("Failed to update comment count for comment {}: {}", commentId, e.getMessage());
        }
    }

    private void updatePostCommentCount(@NotNull Integer postId) {
        try {
            // Find or create public metric record
            Optional<PublicMatric> metricOpt = publicMatricRepository.findByPostId(postId);
            PublicMatric metric;
            
            if (metricOpt.isPresent()) {
                metric = metricOpt.get();
            } else {
                // Create new metric record if not exists
                metric = new PublicMatric();
                Post post = new Post();
                post.setId(postId);
                metric.setPost(post);
                metric.setLikes(0);
                metric.setFavorites(0);
            }
            
            // Update published comment count
            Long commentCount = commentRepository.countByPostIdAndStatus(postId, CommentStatus.PUBLISHED);
            metric.setComments(Math.toIntExact(commentCount));
            publicMatricRepository.save(metric);
            
            log.debug("Updated comment count for post {} to {}", postId, commentCount);
        } catch (Exception e) {
            log.warn("Failed to update comment count for post {}: {}", postId, e.getMessage());
        }
    }

    // endregion

    // region Draft Comment Operations

    @Override
    @Transactional
    public PostComment getDraftBox(@NotNull Integer userId, @NotNull Integer postId, Integer parentCommentId) 
            throws PostNotFoundException, CommentNotFoundException {
        
        log.debug("Getting draft box for user {} on post {} with parent {}", userId, postId, parentCommentId);
        
        // Verify target post exists and is published
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    Optional<Post> existingPost = postRepository.findById(postId);
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + postId);
                    } else {
                        return new PostNotFoundException("Post is not published: " + postId);
                    }
                });

        // Verify parent comment exists if specified
        if (parentCommentId != null) {
            Optional<PostComment> parentComment = commentRepository.findById(parentCommentId);
            if (parentComment.isEmpty()) {
                throw new CommentNotFoundException(parentCommentId);
            }
            // Verify parent comment belongs to the same post and is published
            if (!Objects.equals(parentComment.get().getPost().getId(), postId) ||
                parentComment.get().getStatus() != CommentStatus.PUBLISHED) {
                throw new CommentNotFoundException("Parent comment does not belong to the specified post or is not published");
            }
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if user already has a draft
        Optional<PostComment> existingDraft = commentRepository.findByUserIdAndStatus(userId, CommentStatus.DRAFT);
        
        if (existingDraft.isPresent()) {
            PostComment draft = existingDraft.get();
            
            // Check if the draft is for the same target (post and parent comment)
            boolean sameTarget = Objects.equals(draft.getPost().getId(), postId) &&
                               Objects.equals(draft.getParentPostId(), parentCommentId);
            
            if (sameTarget) {
                // Return existing draft as it's for the same target
                log.debug("Returning existing draft {} for user {}", draft.getId(), userId);
                return draft;
            } else {
                // Overwrite existing draft with new target
                log.debug("Overwriting existing draft {} for user {} with new target", draft.getId(), userId);
                draft.setPost(post);
                draft.setParentPostId(parentCommentId);
                draft.setContent(""); // Reset content for new target
                draft.setMedia(Collections.emptyList()); // Clear media
                return commentRepository.save(draft);
            }
        }
        
        // Create new draft
        PostComment newDraft = new PostComment();
        newDraft.setPost(post);
        newDraft.setUser(user);
        newDraft.setParentPostId(parentCommentId);
        newDraft.setContent("");
        newDraft.setMedia(Collections.emptyList());
        newDraft.setStatus(CommentStatus.DRAFT);
        newDraft.setComments(0);
        newDraft.setLikes(0);
        
        PostComment savedDraft = commentRepository.save(newDraft);
        log.debug("Created new draft {} for user {}", savedDraft.getId(), userId);
        
        return savedDraft;
    }

    @Override
    @Transactional
    public PostComment editDraft(@NotNull Integer userId, @NotNull DraftCommentEditor draftEditor) 
            throws PostNotFoundException, CommentNotFoundException, CommentPermissionDeniedException {
        
        log.debug("Editing draft for user {} with content: {}", userId, draftEditor);
        
        // Get user's current draft
        PostComment draft = commentRepository.findByUserIdAndStatus(userId, CommentStatus.DRAFT)
                .orElseThrow(() -> new CommentNotFoundException("User does not have a comment draft"));
        
        // Check ownership (should always pass since we're finding by userId, but good to be explicit)
        if (!draft.getUser().getId().equals(userId)) {
            throw new CommentPermissionDeniedException(userId, draft.getId(), "edit");
        }
        
        // Check if target has changed, if so get/create new draft box first
        if (!Objects.equals(draft.getPost().getId(), draftEditor.postId()) ||
            !Objects.equals(draft.getParentPostId(), draftEditor.parentCommentId())) {
            
            log.debug("Target changed for user {}, getting new draft box", userId);
            draft = getDraftBox(userId, draftEditor.postId(), draftEditor.parentCommentId());
        }
        
        // Update content
        if (draftEditor.content() != null) {
            draft.setContent(draftEditor.content());
            log.debug("Updated draft content for user {}", userId);
        }
        
        return commentRepository.save(draft);
    }

    @Override
    @Transactional
    public PostComment publishDraft(@NotNull Integer userId) 
            throws CommentNotFoundException, CommentPermissionDeniedException, CommentStatusNotAllowedException, PostStatusNotAllowedException {
        
        log.debug("Publishing draft for user {}", userId);
        
        // Get user's draft
        PostComment draft = commentRepository.findByUserIdAndStatus(userId, CommentStatus.DRAFT)
                .orElseThrow(() -> new CommentNotFoundException("User does not have a comment draft"));
        
        // Check ownership
        if (!draft.getUser().getId().equals(userId)) {
            throw new CommentPermissionDeniedException(userId, draft.getId(), "publish");
        }
        
        // Validate draft status
        if (draft.getStatus() != CommentStatus.DRAFT) {
            throw new CommentStatusNotAllowedException(draft.getId(), draft.getStatus(), "publish");
        }
        
        // Verify target post is still published
        Post post = postRepository.findByIdAndStatus(draft.getPost().getId(), PostStatus.PUBLISHED)
                .orElseThrow(() -> new PostStatusNotAllowedException(draft.getPost().getId(), 
                        null, "comment", "Post is not published"));
        
        // Validate content
        if (!StringUtils.hasText(draft.getContent())) {
            throw new CommentStatusNotAllowedException(draft.getId(), draft.getStatus(), 
                    "publish", "Comment content cannot be empty");
        }
        
        // Verify parent comment still exists and is published (if applicable)
        if (draft.getParentPostId() != null) {
            PostComment parentComment = commentRepository.findById(draft.getParentPostId())
                    .orElseThrow(() -> new CommentNotFoundException("Parent comment no longer exists: " + draft.getParentPostId()));
            
            if (parentComment.getStatus() != CommentStatus.PUBLISHED) {
                throw new CommentStatusNotAllowedException(parentComment.getId(), parentComment.getStatus(),
                        "reply", "Cannot reply to non-published comment");
            }
        }
        
        // Publish the draft
        draft.setStatus(CommentStatus.PUBLISHED);
        PostComment publishedComment = commentRepository.save(draft);
        
        // Update parent comment reply count
        if (draft.getParentPostId() != null) {
            updateParentCommentReplyCount(draft.getParentPostId());
        }
        
        // Update post comment count  
        updatePostCommentCount(draft.getPost().getId());
        
        log.info("Published comment {} from draft for user {}", publishedComment.getId(), userId);
        return publishedComment;
    }

    @Override
    @Transactional
    public void clearDraft(@NotNull Integer userId) 
            throws CommentNotFoundException, CommentPermissionDeniedException {
        
        log.debug("Clearing draft for user {}", userId);
        
        // Get user's draft
        PostComment draft = commentRepository.findByUserIdAndStatus(userId, CommentStatus.DRAFT)
                .orElseThrow(() -> new CommentNotFoundException("User does not have a comment draft"));
        
        // Check ownership
        if (!draft.getUser().getId().equals(userId)) {
            throw new CommentPermissionDeniedException(userId, draft.getId(), "clear");
        }
        
        // Delete the draft
        commentRepository.delete(draft);
        log.debug("Cleared draft {} for user {}", draft.getId(), userId);
    }

    @Override
    public Boolean hasDraft(@NotNull Integer userId) {
        return commentRepository.findByUserIdAndStatus(userId, CommentStatus.DRAFT).isPresent();
    }
    
    private void updateParentCommentReplyCount(@NotNull Integer parentCommentId) {
        updateCommentCount(parentCommentId);
    }

    // endregion

    // region Comment Interaction Operations

    @Override
    public Boolean toggleCommentLike(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException {
        
        log.debug("User {} toggling like for comment {}", userId, commentId);
        
        // Verify comment exists
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        
        try {
            return commentLikeService.toggleCommentLike(userId, commentId);
        } catch (Exception e) {
            log.error("Error toggling comment like for user {} and comment {}: {}", 
                     userId, commentId, e.getMessage(), e);
            throw new RuntimeException("Failed to toggle comment like", e);
        }
    }

    @Override
    public void likeComment(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException, IllegalStateException {
        
        log.debug("User {} liking comment {}", userId, commentId);
        
        // Verify comment exists
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        
        try {
            commentLikeService.likeComment(userId, commentId);
        } catch (Exception e) {
            log.error("Error liking comment for user {} and comment {}: {}", 
                     userId, commentId, e.getMessage(), e);
            throw new RuntimeException("Failed to like comment", e);
        }
    }

    @Override
    public void unlikeComment(@NotNull Integer userId, @NotNull Integer commentId)
            throws CommentNotFoundException, UserNotFoundException, IllegalStateException {
        
        log.debug("User {} unliking comment {}", userId, commentId);
        
        // Verify comment exists
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        
        try {
            commentLikeService.unlikeComment(userId, commentId);
        } catch (Exception e) {
            log.error("Error unliking comment for user {} and comment {}: {}", 
                     userId, commentId, e.getMessage(), e);
            throw new RuntimeException("Failed to unlike comment", e);
        }
    }

    @Override
    public Boolean hasLikedComment(@NotNull Integer userId, @NotNull Integer commentId) {
        return commentLikeService.hasLikedComment(userId, commentId);
    }

    @Override
    public Long getCommentLikeCount(@NotNull Integer commentId) {
        return commentLikeService.getCommentLikeCount(commentId);
    }

    // endregion

}