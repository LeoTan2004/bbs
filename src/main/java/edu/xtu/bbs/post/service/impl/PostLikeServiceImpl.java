package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.notification.dto.NotificationCreateRequest;
import edu.xtu.bbs.notification.model.NotificationPriority;
import edu.xtu.bbs.notification.model.NotificationType;
import edu.xtu.bbs.notification.service.NotificationService;
import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostLike;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostLikeRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.PostLikeService;
import edu.xtu.bbs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PublicMatricRepository publicMatricRepository;
    private final NotificationService notificationService;

    private static final int NOTIFICATION_TITLE_LIMIT = 255;
    private static final int CONTEXT_TITLE_LIMIT = 120;

    @Override
    @Transactional
    public Boolean likePost(Integer userId, Integer postId) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} attempting to like post {}", userId, postId);
        
        // Verify post exists and is published
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    // Check if post exists but not published
                    Optional<Post> existingPost = postRepository.findById(postId);
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + postId);
                    } else {
                        return new PostStatusNotAllowedException(postId, existingPost.get().getStatus(), "like", "Post is not published");
                    }
                });
        
        // Check if already liked
        boolean alreadyLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
        if (alreadyLiked) {
            log.debug("User {} already liked post {}", userId, postId);
            return false; // Already liked
        }
        
        // Create like record
        PostLike like = new PostLike();
        like.setPost(post);
        User user = new User();
        user.setId(userId);
        like.setUser(user);
        like.setCreatedAt(Instant.now());
        
        postLikeRepository.save(like);
        
        // Update public metrics
        updateLikeCount(postId);

        dispatchNotificationForPostLike(userId, post);
        
        log.info("User {} liked post {}", userId, postId);
        return true; // Successfully liked
    }

    @Override
    @Transactional
    public Boolean unlikePost(Integer userId, Integer postId) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} attempting to unlike post {}", userId, postId);
        
        // Verify post exists and is published
        postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    // Check if post exists but not published
                    Optional<Post> existingPost = postRepository.findById(postId);
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + postId);
                    } else {
                        return new PostStatusNotAllowedException(postId, existingPost.get().getStatus(), "unlike", "Post is not published");
                    }
                });
        
        // Check if liked and remove
        int deletedCount = postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        
        if (deletedCount > 0) {
            // Update public metrics
            updateLikeCount(postId);
            log.info("User {} unliked post {}", userId, postId);
            return true; // Successfully unliked
        } else {
            log.debug("User {} had not liked post {}", userId, postId);
            return false; // Was not liked before
        }
    }

    @Override
    public Boolean hasLikedPost(Integer userId, Integer postId) {
        log.debug("Checking if user {} has liked post {}", userId, postId);
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public Page<Post> getLikedPostsByUser(Integer userId, Pageable pageable) {
        log.debug("Getting posts liked by user {}", userId);
        
        Page<PostLike> likes = postLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return likes.map(PostLike::getPost);
    }

    @Override
    public Integer countUserLikedPosts(Integer userId) {
        log.debug("Counting posts liked by user {}", userId);
        return Math.toIntExact(postLikeRepository.countByUserId(userId));
    }

    @Override
    public Integer countPostLikedByUsers(Integer postId) {
        log.debug("Counting likes for post {}", postId);
        return Math.toIntExact(postLikeRepository.countByPostId(postId));
    }

    private void dispatchNotificationForPostLike(Integer likerId, Post post) {
        if (post.getAuthor() == null || Objects.equals(post.getAuthor().getId(), likerId)) {
            return;
        }

        Map<String, Object> context = buildLikeContext(post);
        notificationService.create(new NotificationCreateRequest(
                post.getAuthor().getId(),
                likerId,
                NotificationType.POST_LIKED,
                NotificationPriority.NORMAL,
                truncate("你的帖子《" + post.getTitle() + "》收获新的点赞", NOTIFICATION_TITLE_LIMIT),
                null,
                "/posts/" + post.getId(),
                "POST",
                String.valueOf(post.getId()),
                null,
                context
        ));
    }

    private Map<String, Object> buildLikeContext(Post post) {
        Map<String, Object> context = new HashMap<>();
        putIfNotNull(context, "postId", post.getId());
        putIfNotNull(context, "postTitle", truncate(post.getTitle(), CONTEXT_TITLE_LIMIT));
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

    private void updateLikeCount(Integer postId) {
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
                metric.setComments(0);
                metric.setFavorites(0);
            }
            
            // Update like count
            Long likeCount = postLikeRepository.countByPostId(postId);
            metric.setLikes(Math.toIntExact(likeCount));
            
            publicMatricRepository.save(metric);
            
            log.debug("Updated like count for post {} to {}", postId, likeCount);
        } catch (Exception e) {
            log.warn("Failed to update like count for post {}: {}", postId, e.getMessage());
        }
    }
}