package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.repo.PostFavoriteRepository;
import edu.xtu.bbs.post.repo.PostLikeRepository;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.service.PostBatchService;
import edu.xtu.bbs.post.service.PostFavorService;
import edu.xtu.bbs.post.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostBatchServiceImpl implements PostBatchService {

    private final PostLikeService postLikeService;
    private final PostFavorService postFavorService;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFavoriteRepository postFavoriteRepository;

    @Override
    @Transactional
    public Map<Integer, Boolean> batchLikePosts(Integer userId, List<Integer> postIds) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} batch liking {} posts", userId, postIds.size());
        
        // Validate all posts exist and are published
        validatePostsExistAndPublished(postIds);
        
        Map<Integer, Boolean> results = new HashMap<>();
        
        for (Integer postId : postIds) {
            try {
                Boolean result = postLikeService.likePost(userId, postId);
                results.put(postId, result);
            } catch (Exception e) {
                log.warn("Failed to like post {} for user {}: {}", postId, userId, e.getMessage());
                results.put(postId, false);
            }
        }
        
        log.info("User {} batch liked {} posts, {} successful", userId, postIds.size(), 
                 results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        
        return results;
    }

    @Override
    @Transactional
    public Map<Integer, Boolean> batchUnlikePosts(Integer userId, List<Integer> postIds) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} batch unliking {} posts", userId, postIds.size());
        
        // Validate all posts exist and are published
        validatePostsExistAndPublished(postIds);
        
        Map<Integer, Boolean> results = new HashMap<>();
        
        for (Integer postId : postIds) {
            try {
                Boolean result = postLikeService.unlikePost(userId, postId);
                results.put(postId, result);
            } catch (Exception e) {
                log.warn("Failed to unlike post {} for user {}: {}", postId, userId, e.getMessage());
                results.put(postId, false);
            }
        }
        
        log.info("User {} batch unliked {} posts, {} successful", userId, postIds.size(),
                 results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        
        return results;
    }

    @Override
    @Transactional
    public Map<Integer, Boolean> batchFavorPosts(Integer userId, List<Integer> postIds) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} batch favoring {} posts", userId, postIds.size());
        
        // Validate all posts exist and are published
        validatePostsExistAndPublished(postIds);
        
        Map<Integer, Boolean> results = new HashMap<>();
        
        for (Integer postId : postIds) {
            try {
                Boolean result = postFavorService.favorPost(userId, postId);
                results.put(postId, result);
            } catch (Exception e) {
                log.warn("Failed to favor post {} for user {}: {}", postId, userId, e.getMessage());
                results.put(postId, false);
            }
        }
        
        log.info("User {} batch favored {} posts, {} successful", userId, postIds.size(),
                 results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        
        return results;
    }

    @Override
    @Transactional
    public Map<Integer, Boolean> batchUnfavorPosts(Integer userId, List<Integer> postIds) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        log.debug("User {} batch unfavoring {} posts", userId, postIds.size());
        
        // Validate all posts exist and are published
        validatePostsExistAndPublished(postIds);
        
        Map<Integer, Boolean> results = new HashMap<>();
        
        for (Integer postId : postIds) {
            try {
                Boolean result = postFavorService.unfavorPost(userId, postId);
                results.put(postId, result);
            } catch (Exception e) {
                log.warn("Failed to unfavor post {} for user {}: {}", postId, userId, e.getMessage());
                results.put(postId, false);
            }
        }
        
        log.info("User {} batch unfavored {} posts, {} successful", userId, postIds.size(),
                 results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
        
        return results;
    }

    @Override
    public Map<Integer, Boolean> batchCheckLikeStatus(Integer userId, List<Integer> postIds) {
        log.debug("Checking like status for user {} on {} posts", userId, postIds.size());

        Map<Integer, Boolean> results = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return results;
        }

        Set<Integer> likedPostIds = new HashSet<>(
                postLikeRepository.findPostIdsByUserIdAndPostIdIn(userId, postIds)
        );

        for (Integer postId : postIds) {
            results.put(postId, likedPostIds.contains(postId));
        }

        return results;
    }

    @Override
    public Map<Integer, Boolean> batchCheckFavorStatus(Integer userId, List<Integer> postIds) {
        log.debug("Checking favor status for user {} on {} posts", userId, postIds.size());

        Map<Integer, Boolean> results = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return results;
        }

        Set<Integer> favoredPostIds = new HashSet<>(
                postFavoriteRepository.findPostIdsByUserIdAndPostIdIn(userId, postIds)
        );

        for (Integer postId : postIds) {
            results.put(postId, favoredPostIds.contains(postId));
        }

        return results;
    }

    private void validatePostsExistAndPublished(List<Integer> postIds) 
            throws PostNotFoundException, PostStatusNotAllowedException {
        
        List<edu.xtu.bbs.post.model.Post> posts = postRepository.findByIdsAndStatus(postIds, PostStatus.PUBLISHED);
        
        if (posts.size() != postIds.size()) {
            // Find missing or non-published posts
            Set<Integer> foundPostIds = posts.stream()
                    .map(edu.xtu.bbs.post.model.Post::getId)
                    .collect(Collectors.toSet());
            
            List<Integer> missingIds = postIds.stream()
                    .filter(id -> !foundPostIds.contains(id))
                    .toList();
            
            // Check if posts exist but are not published
            List<edu.xtu.bbs.post.model.Post> existingPosts = postRepository.findAllById(missingIds);
            Set<Integer> existingPostIds = existingPosts.stream()
                    .map(edu.xtu.bbs.post.model.Post::getId)
                    .collect(Collectors.toSet());
            
            List<Integer> notFoundIds = missingIds.stream()
                    .filter(id -> !existingPostIds.contains(id))
                    .toList();
            
            if (!notFoundIds.isEmpty()) {
                throw new PostNotFoundException("Posts not found: " + notFoundIds);
            }
            
            List<Integer> notPublishedIds = missingIds.stream()
                    .filter(existingPostIds::contains)
                    .toList();
            
            if (!notPublishedIds.isEmpty()) {
                throw new PostStatusNotAllowedException(notPublishedIds.get(0), PostStatus.PUBLISHED, "batch operation", "Posts not published: " + notPublishedIds);
            }
        }
    }
}