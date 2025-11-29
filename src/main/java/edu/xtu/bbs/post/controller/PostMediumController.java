package edu.xtu.bbs.post.controller;

import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.dto.PostMediumUploadRequest;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import edu.xtu.bbs.post.service.PostMediumService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostMediumController {

    private final PostMediumService postMediumService;
    private final AuthenticationService authenticationService;

    public PostMediumController(PostMediumService postMediumService,
                                AuthenticationService authenticationService) {
        this.postMediumService = postMediumService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/{postId}/media")
    public List<Medium> getPostMedia(@PathVariable Integer postId) {
        final User currentUser = authenticationService.getCurrentUser();
        final Integer userId = currentUser != null ? currentUser.getId() : null;

        if (userId != null) {
            return postMediumService.getMediaByPostId(userId, postId);
        } else {
            return postMediumService.getMediaByPostId(postId);
        }
    }

    @PostMapping("/{postId}/media")
    public MediumUploadResult uploadMedium(@PathVariable Integer postId,
                                           @Valid @RequestBody PostMediumUploadRequest uploadRequest)
            throws PostNotFoundException, UploadNotPermittedException, PostStatusNotAllowedException,
            UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException {
        if (uploadRequest == null) {
            throw new IllegalArgumentException("Invalid upload request");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postMediumService.uploadMediumToDraft(currentUser.getId(), postId, uploadRequest);
    }

    @DeleteMapping("/{postId}/media/{mediumId}")
    public List<Medium> deleteMedium(@PathVariable Integer postId,
                                     @PathVariable String mediumId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, DeletionFailedException {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postMediumService.deleteMediumFromDraft(currentUser.getId(), postId, mediumId);
    }

    @PatchMapping("/{postId}/media/{mediumId}")
    public Medium updateMediumMetadata(@PathVariable Integer postId,
                                       @PathVariable String mediumId,
                                       @RequestParam String type)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, UnsupportedMediumTypeException {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Medium type cannot be empty");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        return postMediumService.updateMediumMetadata(currentUser.getId(), postId, mediumId, type);
    }

    @GetMapping("/media/{mediumId}/access")
    public Boolean hasAccessPermission(@PathVariable String mediumId) {
        final User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return postMediumService.hasAccessPermission(currentUser.getId(), mediumId);
    }
}