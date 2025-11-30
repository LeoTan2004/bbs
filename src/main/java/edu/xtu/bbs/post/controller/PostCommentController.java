package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.CommentResponse;
import edu.xtu.bbs.post.dto.CreateCommentRequest;
import edu.xtu.bbs.post.dto.DraftCommentEditor;
import edu.xtu.bbs.post.dto.UpdateCommentRequest;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.service.PostCommentService;
import edu.xtu.bbs.user.exception.UserNotFoundException;
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
@RequestMapping("/comments")
public class PostCommentController {

    private final PostCommentService postCommentService;
    private final AuthenticationService authenticationService;

    public PostCommentController(PostCommentService postCommentService,
                                 AuthenticationService authenticationService) {
        this.postCommentService = postCommentService;
        this.authenticationService = authenticationService;
    }

    // region Comment CRUD Operations

    @PostMapping
    public PostComment createComment(@Valid @RequestBody CreateCommentRequest request)
            throws PostNotFoundException, PostStatusNotAllowedException, CommentNotFoundException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid comment creation request");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.createComment(currentUser.getId(), request);
    }

    @PostMapping("/direct")
    public PostComment createCommentDirect(@Valid @RequestBody CreateCommentRequest request)
            throws PostNotFoundException, PostStatusNotAllowedException, CommentNotFoundException {
        return createComment(request);
    }

    @PutMapping("/{commentId}")
    public PostComment updateComment(@PathVariable Integer commentId,
                                     @Valid @RequestBody UpdateCommentRequest request)
            throws CommentNotFoundException, CommentPermissionDeniedException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid comment update request");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.updateComment(currentUser.getId(), commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Integer commentId)
            throws CommentNotFoundException, CommentPermissionDeniedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        postCommentService.deleteComment(currentUser.getId(), commentId);
    }

    // endregion

    // region Comment Retrieval

    @GetMapping("/{commentId}")
    public CommentResponse getCommentById(@PathVariable Integer commentId)
            throws CommentNotFoundException {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer userId = currentUser != null ? currentUser.getId() : null;

        return postCommentService.getCommentResponseById(commentId, userId);
    }

    @GetMapping("/post/{postId}")
    public Page<CommentResponse> getCommentsByPost(@PathVariable Integer postId,
                                                   @PageableDefault Pageable pageable)
            throws PostNotFoundException {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer userId = currentUser != null ? currentUser.getId() : null;

        return postCommentService.getCommentResponsesByPost(postId, userId, pageable);
    }

    @GetMapping("/{commentId}/replies")
    public Page<CommentResponse> getRepliesByComment(@PathVariable Integer commentId,
                                                     @PageableDefault Pageable pageable)
            throws CommentNotFoundException {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer userId = currentUser != null ? currentUser.getId() : null;

        return postCommentService.getReplyResponsesByComment(commentId, userId, pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<CommentResponse> getCommentsByUser(@PathVariable Integer userId,
                                                   @PageableDefault Pageable pageable) {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer currentUserId = currentUser != null ? currentUser.getId() : null;

        return postCommentService.getCommentResponsesByUser(userId, currentUserId, pageable);
    }

    // endregion

    // region Draft Operations

    @GetMapping("/draft")
    public PostComment getDraftBox(@RequestParam Integer postId,
                                   @RequestParam(required = false) Integer parentCommentId)
            throws PostNotFoundException, CommentNotFoundException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.getDraftBox(currentUser.getId(), postId, parentCommentId);
    }

    @PutMapping("/draft")
    public PostComment editDraft(@Valid @RequestBody DraftCommentEditor draftEditor)
            throws PostNotFoundException, CommentNotFoundException, CommentPermissionDeniedException {
        if (draftEditor == null) {
            throw new IllegalArgumentException("Invalid draft comment editor");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.editDraft(currentUser.getId(), draftEditor);
    }

    @PostMapping("/draft/publish")
    public PostComment publishDraft()
            throws CommentNotFoundException, CommentPermissionDeniedException,
            CommentStatusNotAllowedException, PostStatusNotAllowedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.publishDraft(currentUser.getId());
    }

    @DeleteMapping("/draft")
    public void clearDraft()
            throws CommentNotFoundException, CommentPermissionDeniedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        postCommentService.clearDraft(currentUser.getId());
    }

    @GetMapping("/draft/exists")
    public Boolean hasDraft() {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.hasDraft(currentUser.getId());
    }

    // endregion

    // region Comment Interaction

    @PostMapping("/{commentId}/like")
    public Boolean toggleCommentLike(@PathVariable Integer commentId)
            throws CommentNotFoundException, UserNotFoundException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentService.toggleCommentLike(currentUser.getId(), commentId);
    }

    @GetMapping("/{commentId}/like/status")
    public Boolean hasLikedComment(@PathVariable Integer commentId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return postCommentService.hasLikedComment(currentUser.getId(), commentId);
    }

    @GetMapping("/{commentId}/like/count")
    public Long getCommentLikeCount(@PathVariable Integer commentId) {
        return postCommentService.getCommentLikeCount(commentId);
    }

    // endregion

    // region Statistics

    @GetMapping("/post/{postId}/count")
    public Integer countCommentsByPost(@PathVariable Integer postId) {
        return postCommentService.countCommentsByPost(postId);
    }

    @GetMapping("/post/{postId}/count/top-level")
    public Integer countTopLevelCommentsByPost(@PathVariable Integer postId) {
        return postCommentService.countTopLevelCommentsByPost(postId);
    }

    @GetMapping("/{commentId}/replies/count")
    public Integer countRepliesByComment(@PathVariable Integer commentId) {
        return postCommentService.countRepliesByComment(commentId);
    }

    @GetMapping("/user/{userId}/count")
    public Integer countCommentsByUser(@PathVariable Integer userId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new SecurityException("Can only view your own statistics");
        }

        return postCommentService.countCommentsByUser(userId);
    }

    // endregion
}