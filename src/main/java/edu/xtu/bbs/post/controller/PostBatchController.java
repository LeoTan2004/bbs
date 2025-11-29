package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.exception.PostNotFoundException;
import edu.xtu.bbs.post.exception.PostStatusNotAllowedException;
import edu.xtu.bbs.post.service.PostBatchService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/posts/batch")
public class PostBatchController {

    private final PostBatchService postBatchService;
    private final AuthenticationService authenticationService;

    public PostBatchController(PostBatchService postBatchService,
                               AuthenticationService authenticationService) {
        this.postBatchService = postBatchService;
        this.authenticationService = authenticationService;
    }

    // region Batch Like Operations

    @PostMapping("/like")
    public Map<Integer, Boolean> batchLikePosts(@RequestBody List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchLikePosts(currentUser.getId(), postIds);
    }

    @DeleteMapping("/like")
    public Map<Integer, Boolean> batchUnlikePosts(@RequestBody List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchUnlikePosts(currentUser.getId(), postIds);
    }

    @GetMapping("/like/status")
    public Map<Integer, Boolean> batchCheckLikeStatus(@RequestParam List<Integer> postIds) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchCheckLikeStatus(currentUser.getId(), postIds);
    }

    // endregion

    // region Batch Favor Operations

    @PostMapping("/favor")
    public Map<Integer, Boolean> batchFavorPosts(@RequestBody List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchFavorPosts(currentUser.getId(), postIds);
    }

    @DeleteMapping("/favor")
    public Map<Integer, Boolean> batchUnfavorPosts(@RequestBody List<Integer> postIds)
            throws PostNotFoundException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchUnfavorPosts(currentUser.getId(), postIds);
    }

    @GetMapping("/favor/status")
    public Map<Integer, Boolean> batchCheckFavorStatus(@RequestParam List<Integer> postIds) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postBatchService.batchCheckFavorStatus(currentUser.getId(), postIds);
    }

    // endregion
}