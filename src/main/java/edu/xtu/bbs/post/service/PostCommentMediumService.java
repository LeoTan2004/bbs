package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.CommentMediumUploadRequest;
import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Post Comment Medium Service Interface
 * <p>
 * Handles operations related to comment media attachments.
 * Similar to PostMediumService but for comments.
 * </p>
 */
public interface PostCommentMediumService {

    /**
     * Get media by comment id
     *
     * @param commentId the comment id
     * @return list of media
     */
    List<Medium> getMediaByCommentId(@NotNull Integer commentId);

    /**
     * Get media by comment id with access control
     *
     * @param userId the requesting user id
     * @param commentId the comment id
     * @return list of media that the user has permission to access
     */
    List<Medium> getMediaByCommentId(@NotNull Integer userId, @NotNull Integer commentId);

    /**
     * Upload media to a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @param uploadRequest the upload request
     * @return the upload result
     * @throws CommentNotFoundException when the comment is not found
     * @throws UploadNotPermittedException when the user doesn't have permission to upload to this comment
     * @throws UnsupportedMediumTypeException when the file type is not supported
     * @throws MediumSizeExceededException when the file size exceeds the limit
     * @throws TooManyMediaException when the comment already has too many media files
     */
    MediumUploadResult uploadMediumToComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull CommentMediumUploadRequest uploadRequest)
            throws CommentNotFoundException, UploadNotPermittedException, UnsupportedMediumTypeException,
            MediumSizeExceededException, TooManyMediaException;

    /**
     * Delete medium from a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @param mediumId the medium id
     * @return the updated list of media
     * @throws CommentNotFoundException when the comment is not found
     * @throws ModifyNotPermittedException when the user doesn't have permission to modify this comment
     * @throws DeletionFailedException when the medium deletion fails
     */
    List<Medium> deleteMediumFromComment(@NotNull Integer userId, @NotNull Integer commentId, @NotNull String mediumId)
            throws CommentNotFoundException, ModifyNotPermittedException, DeletionFailedException;

    /**
     * Update medium metadata for a comment
     *
     * @param userId the user id
     * @param commentId the comment id
     * @param mediumId the medium id
     * @param newType the new medium type
     * @return the updated medium
     * @throws CommentNotFoundException when the comment is not found
     * @throws ModifyNotPermittedException when the user doesn't have permission to modify this comment
     * @throws UnsupportedMediumTypeException when the medium type is not supported
     */
    Medium updateMediumMetadata(@NotNull Integer userId, @NotNull Integer commentId, @NotNull String mediumId, @NotNull String newType)
            throws CommentNotFoundException, ModifyNotPermittedException, UnsupportedMediumTypeException;

    /**
     * Check if user has permission to access the comment medium
     *
     * @param userId the user id
     * @param mediumId the medium id
     * @return true if user has access permission
     */
    Boolean hasAccessPermission(@NotNull Integer userId, @NotNull String mediumId);

}