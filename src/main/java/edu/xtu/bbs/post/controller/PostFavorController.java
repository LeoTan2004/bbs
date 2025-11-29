package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.service.PostFavorService;
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
public class PostFavorController {

    private final PostFavorService postFavorService;
    private final AuthenticationService authenticationService;

    public PostFavorController(PostFavorService postFavorService,
                               AuthenticationService authenticationService) {
        this.postFavorService = postFavorService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/{postId}/favor")
    public Boolean favorPost(@PathVariable Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postFavorService.favorPost(currentUser.getId(), postId);
    }

    @DeleteMapping("/{postId}/favor")
    public Boolean unfavorPost(@PathVariable Integer postId)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postFavorService.unfavorPost(currentUser.getId(), postId);
    }

    @GetMapping("/{postId}/favor/status")
    public Boolean isPostFavored(@PathVariable Integer postId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return postFavorService.isPostFavored(currentUser.getId(), postId);
    }

    @GetMapping("/{postId}/favor/count")
    public Integer getPostFavorCount(@PathVariable Integer postId) {
        return postFavorService.countPostFavoredByUsers(postId);
    }

    @GetMapping("/favored")
    public Page<Post> getFavoredPosts(@PageableDefault Pageable pageable) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postFavorService.getUserFavoredPosts(currentUser.getId(), pageable);
    }

    @GetMapping("/user/{userId}/favored")
    public Page<Post> getUserFavoredPosts(@PathVariable Integer userId,
                                          @PageableDefault Pageable pageable) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new SecurityException("Can only view your own favored posts");
        }

        return postFavorService.getUserFavoredPosts(userId, pageable);
    }

    @GetMapping("/user/{userId}/favored/count")
    public Integer getUserFavoredPostsCount(@PathVariable Integer userId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new SecurityException("Can only view your own statistics");
        }

        return postFavorService.countUserFavoredPosts(userId);
    }
}