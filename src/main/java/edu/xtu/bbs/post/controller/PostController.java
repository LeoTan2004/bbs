package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.DraftContentEditor;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.service.PostService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final AuthenticationService authenticationService;

    public PostController(PostService postService, AuthenticationService authenticationService) {
        this.postService = postService;
        this.authenticationService = authenticationService;
    }

    // region Draft Operations

    @GetMapping("/draft")
    public Post getDraft() {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }
        return postService.getDraft(currentUser.getId());
    }

    @PutMapping("/draft/{draftId}")
    public Post editDraft(@PathVariable Integer draftId,
                          @Valid @RequestBody DraftContentEditor editor)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException {
        if (editor == null) {
            throw new IllegalArgumentException("Invalid draft content editor");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.editDraft(currentUser.getId(), draftId, editor);
    }

    @PostMapping("/draft/{draftId}/publish")
    public Post publishDraft(@PathVariable Integer draftId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.publishDraft(currentUser.getId(), draftId);
    }

    // endregion

    // region Post Retrieval

    @GetMapping
    public Page<Post> getAllPublishedPosts(@PageableDefault Pageable pageable,
                                           @RequestParam(required = false) String category) {
        if (category != null && !category.trim().isEmpty()) {
            return postService.getAllPublishedPostsByCategory(category, pageable);
        }
        return postService.getHotPublishedPosts(pageable);
    }

    @GetMapping("/hot")
    public Page<Post> getHotPublishedPosts(@PageableDefault Pageable pageable) {
        return postService.getHotPublishedPosts(pageable);
    }

    @GetMapping("/search")
    public Page<Post> searchPublishedPosts(@RequestParam String keyword,
                                           @PageableDefault Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty");
        }
        return postService.searchPublishedPosts(keyword, pageable);
    }

    @GetMapping("/{postId}")
    public Post getPostById(@PathVariable Integer postId) {
        final Post post = postService.getPublishedPostById(postId);
        if (post == null) {
            throw new PostNotFoundException("Post not found with id: " + postId);
        }
        return post;
    }

    @GetMapping("/user/{userId}")
    public Page<Post> getUserPosts(@PathVariable Integer userId,
                                   @PageableDefault Pageable pageable) {
        return postService.getPublishedPostsByUser(userId, pageable);
    }

    // endregion

    // region Post Management

    @DeleteMapping("/{postId}")
    public Boolean deletePost(@PathVariable Integer postId)
            throws PostNotFoundException, ModifyNotPermittedException, DeletionFailedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.deletePost(currentUser.getId(), postId);
    }

    @PatchMapping("/{postId}/toggle-status")
    public Post togglePostStatus(@PathVariable Integer postId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.togglePostStatus(currentUser.getId(), postId);
    }

    @PatchMapping("/{postId}/to-draft")
    public Post convertToDraft(@PathVariable Integer postId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.convertToDraft(currentUser.getId(), postId);
    }

    @PatchMapping("/{postId}/publish")
    public Post convertToPublished(@PathVariable Integer postId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postService.convertToPublished(currentUser.getId(), postId);
    }

    // endregion
}