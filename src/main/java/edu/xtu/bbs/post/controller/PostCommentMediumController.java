package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.service.PostCommentMediumService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
public class PostCommentMediumController {

    private final PostCommentMediumService postCommentMediumService;
    private final AuthenticationService authenticationService;

    public PostCommentMediumController(PostCommentMediumService postCommentMediumService,
                                       AuthenticationService authenticationService) {
        this.postCommentMediumService = postCommentMediumService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/{commentId}/media")
    public List<Medium> getCommentMedia(@PathVariable Integer commentId) {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer userId = currentUser != null ? currentUser.getId() : null;

        if (userId != null) {
            return postCommentMediumService.getMediaByCommentId(userId, commentId);
        } else {
            return postCommentMediumService.getMediaByCommentId(commentId);
        }
    }

    @PostMapping("/{commentId}/media")
    public MediumUploadResult uploadMedium(@PathVariable Integer commentId,
                                           @Valid @RequestBody CommentMediumUploadRequest uploadRequest)
            throws CommentNotFoundException, UploadNotPermittedException, UnsupportedMediumTypeException,
            MediumSizeExceededException, TooManyMediaException {
        if (uploadRequest == null) {
            throw new IllegalArgumentException("Invalid upload request");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentMediumService.uploadMediumToComment(currentUser.getId(), commentId, uploadRequest);
    }

    @DeleteMapping("/{commentId}/media/{mediumId}")
    public List<Medium> deleteMedium(@PathVariable Integer commentId,
                                     @PathVariable String mediumId)
            throws CommentNotFoundException, ModifyNotPermittedException, DeletionFailedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postCommentMediumService.deleteMediumFromComment(currentUser.getId(), commentId, mediumId);
    }
}