package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostFavorite;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostFavoriteRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.PostFavorService;
import edu.xtu.bbs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostFavorServiceImpl implements PostFavorService {

    private final PostFavoriteRepository postFavoriteRepository;
    private final PostRepository postRepository;
    private final PublicMatricRepository publicMatricRepository;

    @Override
    @Transactional
    public Boolean favorPost(Integer userId, Integer postId) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} attempting to favor post {}", userId, postId);
        
        // Verify post exists and is published
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    // Check if post exists but not published
                    Optional<Post> existingPost = postRepository.findById(postId);
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + postId);
                    } else {
                        return new PostStatusNotAllowedException(postId, existingPost.get().getStatus(), "favor", "Post is not published");
                    }
                });
        
        // Check if already favorited
        boolean alreadyFavorited = postFavoriteRepository.existsByPostIdAndUserId(postId, userId);
        if (alreadyFavorited) {
            log.debug("User {} already favorited post {}", userId, postId);
            return false; // Already favorited
        }
        
        // Create favorite record
        PostFavorite favorite = new PostFavorite();
        favorite.setPost(post);
        User user = new User();
        user.setId(userId);
        favorite.setUser(user);
        favorite.setCreatedAt(Instant.now());
        
        postFavoriteRepository.save(favorite);
        
        // Update public metrics
        updateFavoriteCount(postId);
        
        log.info("User {} favorited post {}", userId, postId);
        return true; // Successfully favorited
    }

    @Override
    @Transactional
    public Boolean unfavorPost(Integer userId, Integer postId) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} attempting to unfavor post {}", userId, postId);
        
        // Verify post exists and is published
        postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> {
                    // Check if post exists but not published
                    Optional<Post> existingPost = postRepository.findById(postId);
                    if (existingPost.isEmpty()) {
                        return new PostNotFoundException("Post not found with id: " + postId);
                    } else {
                        return new PostStatusNotAllowedException(postId, existingPost.get().getStatus(), "unfavor", "Post is not published");
                    }
                });
        
        // Check if favorited and remove
        int deletedCount = postFavoriteRepository.deleteByPostIdAndUserId(postId, userId);
        
        if (deletedCount > 0) {
            // Update public metrics
            updateFavoriteCount(postId);
            log.info("User {} unfavorited post {}", userId, postId);
            return true; // Successfully unfavorited
        } else {
            log.debug("User {} had not favorited post {}", userId, postId);
            return false; // Was not favorited before
        }
    }

    @Override
    public Boolean isPostFavored(Integer userId, Integer postId) {
        log.debug("Checking if user {} has favorited post {}", userId, postId);
        return postFavoriteRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public Page<Post> getUserFavoredPosts(Integer userId, Pageable pageable) {
        log.debug("Getting posts favorited by user {}", userId);
        
        Page<PostFavorite> favorites = postFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return favorites.map(PostFavorite::getPost);
    }

    @Override
    public Integer countUserFavoredPosts(Integer userId) {
        log.debug("Counting posts favorited by user {}", userId);
        return Math.toIntExact(postFavoriteRepository.countByUserId(userId));
    }

    @Override
    public Integer countPostFavoredByUsers(Integer postId) {
        log.debug("Counting favorites for post {}", postId);
        return Math.toIntExact(postFavoriteRepository.countByPostId(postId));
    }

    private void updateFavoriteCount(Integer postId) {
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
                metric.setLikes(0);
            }
            
            // Update favorite count
            Long favoriteCount = postFavoriteRepository.countByPostId(postId);
            metric.setFavorites(Math.toIntExact(favoriteCount));
            
            publicMatricRepository.save(metric);
            
            log.debug("Updated favorite count for post {} to {}", postId, favoriteCount);
        } catch (Exception e) {
            log.warn("Failed to update favorite count for post {}: {}", postId, e.getMessage());
        }
    }
}