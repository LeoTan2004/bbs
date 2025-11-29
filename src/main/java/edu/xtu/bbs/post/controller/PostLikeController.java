package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.service.PostLikeService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final AuthenticationService authenticationService;

    public PostLikeController(PostLikeService postLikeService,
                              AuthenticationService authenticationService) {
        this.postLikeService = postLikeService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/{postId}/like")
    public Boolean likePost(@PathVariable Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postLikeService.likePost(currentUser.getId(), postId);
    }

    @DeleteMapping("/{postId}/like")
    public Boolean unlikePost(@PathVariable Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postLikeService.unlikePost(currentUser.getId(), postId);
    }

    @GetMapping("/{postId}/like/status")
    public Boolean hasLikedPost(@PathVariable Integer postId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return postLikeService.hasLikedPost(currentUser.getId(), postId);
    }

    @GetMapping("/{postId}/like/count")
    public Integer getPostLikeCount(@PathVariable Integer postId) {
        return postLikeService.countPostLikedByUsers(postId);
    }

    @GetMapping("/liked")
    public Page<Post> getLikedPosts(@PageableDefault Pageable pageable) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postLikeService.getLikedPostsByUser(currentUser.getId(), pageable);
    }

    @GetMapping("/user/{userId}/liked")
    public Page<Post> getUserLikedPosts(@PathVariable Integer userId,
                                        @PageableDefault Pageable pageable) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new SecurityException("Can only view your own liked posts");
        }

        return postLikeService.getLikedPostsByUser(userId, pageable);
    }

    @GetMapping("/user/{userId}/liked/count")
    public Integer getUserLikedPostsCount(@PathVariable Integer userId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new SecurityException("Can only view your own statistics");
        }

        return postLikeService.countUserLikedPosts(userId);
    }
}