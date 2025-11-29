package edu.xtu.bbs.post.model;

/**
 * Comment Status Enumeration
 * <p>
 * Defines the possible states of a comment in the system.
 * </p>
 */
public enum CommentStatus {
    
    /**
     * Draft comment - not visible to others, can be edited
     */
    DRAFT,
    
    /**
     * Published comment - visible to others, can be read
     */
    PUBLISHED,
    
    /**
     * Deleted comment - marked as deleted, not visible
     */
    DELETED
}