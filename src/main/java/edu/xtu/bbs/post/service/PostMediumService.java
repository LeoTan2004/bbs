package edu.xtu.bbs.post.service;

import edu.xtu.bbs.post.dto.MediumUploadResult;
import edu.xtu.bbs.post.dto.PostMediumUploadRequest;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Medium;

import java.util.List;

public interface PostMediumService {

    /**
     * Get media by post id
     *
     * @param postId the post id
     * @return list of media
     */
    List<Medium> getMediaByPostId(Integer postId);

    /**
     * Get media by post id with access control
     *
     * @param userId the requesting user id
     * @param postId the post id
     * @return list of media that the user has permission to access
     */
    List<Medium> getMediaByPostId(Integer userId, Integer postId);

    /**
     * Upload media to a draft
     *
     * @param userId        the user id
     * @param postId        the draft post id
     * @param uploadRequest the upload request
     * @return the upload result
     * @throws PostNotFoundException          when the draft post is not found
     * @throws UploadNotPermittedException    when the user doesn't have permission to upload to this draft
     * @throws PostStatusNotAllowedException  when the post is not in draft status
     * @throws UnsupportedMediumTypeException when the file type is not supported
     * @throws MediumSizeExceededException    when the file size exceeds the limit
     * @throws TooManyMediaException          when the post already has too many media files
     */
    MediumUploadResult uploadMediumToDraft(Integer userId, Integer postId, PostMediumUploadRequest uploadRequest)
            throws PostNotFoundException, UploadNotPermittedException, PostStatusNotAllowedException,
            UnsupportedMediumTypeException, MediumSizeExceededException, TooManyMediaException;

    /**
     * Delete medium from a draft
     *
     * @param userId   the user id
     * @param postId   the draft post id
     * @param mediumId the medium id
     * @return the updated list of media
     * @throws PostNotFoundException         when the draft post is not found
     * @throws ModifyNotPermittedException   when the user doesn't have permission to modify this draft
     * @throws PostStatusNotAllowedException when the post is not in draft status
     * @throws DeletionFailedException       when the medium deletion fails
     */
    List<Medium> deleteMediumFromDraft(Integer userId, Integer postId, Integer mediumId)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, DeletionFailedException;

    /**
     * Update medium metadata (only for draft posts)
     *
     * @param userId   the user id
     * @param postId   the draft post id
     * @param mediumId the medium id
     * @param newType  the new medium type
     * @return the updated medium
     * @throws PostNotFoundException         when the draft post is not found
     * @throws ModifyNotPermittedException   when the user doesn't have permission to modify this draft
     * @throws PostStatusNotAllowedException when the post is not in draft status
     * @throws UnsupportedMediumTypeException when the medium type is not supported
     */
    Medium updateMediumMetadata(Integer userId, Integer postId, Integer mediumId, String newType)
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, UnsupportedMediumTypeException;

    /**
     * Check if user has permission to access the medium
     *
     * @param userId   the user id
     * @param mediumId the medium id
     * @return true if user has access permission
     */
    Boolean hasAccessPermission(Integer userId, Integer mediumId);

}
