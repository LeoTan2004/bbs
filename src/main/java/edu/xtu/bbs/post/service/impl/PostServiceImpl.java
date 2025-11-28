package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.dto.DraftContentEditor;
import edu.xtu.bbs.post.exception.*;
import edu.xtu.bbs.post.model.Post;
import edu.xtu.bbs.post.model.PostStatus;
import edu.xtu.bbs.post.model.PublicMatric;
import edu.xtu.bbs.post.repo.PostRepository;
import edu.xtu.bbs.post.repo.PublicMatricRepository;
import edu.xtu.bbs.post.service.PostService;
import edu.xtu.bbs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PublicMatricRepository publicMatricRepository;

    // Simple content filter - in production, use more sophisticated filtering
    private static final String[] SENSITIVE_KEYWORDS = {"spam", "abuse", "illegal"};

    @Override
    @Transactional
    public Post getDraft(Integer userId) {
        log.debug("Getting draft for user: {}", userId);
        
        // Find existing draft for user
        Optional<Post> existingDraft = postRepository.findByAuthorIdAndStatus(userId, PostStatus.DRAFT)
                .stream()
                .findFirst(); // Get the latest draft
        
        if (existingDraft.isPresent()) {
            return existingDraft.get();
        }
        
        // Create new draft
        Post newDraft = new Post();
        User author = new User();
        author.setId(userId);
        newDraft.setAuthor(author);
        newDraft.setTitle("New Draft");
        newDraft.setContent("");
        newDraft.setStatus(PostStatus.DRAFT);
        newDraft.setPossiblySensitive(false);
        
        Post savedDraft = postRepository.save(newDraft);
        log.debug("Created new draft with id: {} for user: {}", savedDraft.getId(), userId);
        
        return savedDraft;
    }

    @Override
    @Transactional
    public Post editDraft(Integer userId, Integer draftId, DraftContentEditor draftContentEditor) 
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException {
        
        log.debug("Editing draft {} for user {}", draftId, userId);
        
        Post draft = postRepository.findById(draftId)
                .orElseThrow(() -> new PostNotFoundException("Draft not found with id: " + draftId));
        
        // Check ownership
        if (!draft.getAuthor().getId().equals(userId)) {
            throw new ModifyNotPermittedException(draftId, userId, "User does not own draft");
        }
        
        // Check status
        if (draft.getStatus() != PostStatus.DRAFT) {
            throw new PostStatusNotAllowedException(draftId, draft.getStatus(), "edit");
        }
        
        // Check for sensitive content
        checkSensitiveContent(draftContentEditor.title(), draftContentEditor.content());
        
        // Update draft
        if (StringUtils.hasText(draftContentEditor.title())) {
            draft.setTitle(draftContentEditor.title());
        }
        if (draftContentEditor.content() != null) {
            draft.setContent(draftContentEditor.content());
        }
        if (StringUtils.hasText(draftContentEditor.category())) {
            draft.setCategory(draftContentEditor.category());
        }
        
        Post savedDraft = postRepository.save(draft);
        log.debug("Updated draft {} for user {}", draftId, userId);
        
        return savedDraft;
    }

    @Override
    @Transactional
    public Post publishDraft(Integer userId, Integer draftId) 
            throws PostNotFoundException, ModifyNotPermittedException, PostStatusNotAllowedException, SensitiveContentException {
        
        log.debug("Publishing draft {} for user {}", draftId, userId);
        
        Post draft = postRepository.findById(draftId)
                .orElseThrow(() -> new PostNotFoundException("Draft not found with id: " + draftId));
        
        // Check ownership
        if (!draft.getAuthor().getId().equals(userId)) {
            throw new ModifyNotPermittedException(draftId, userId, "User does not own draft");
        }
        
        // Check status
        if (draft.getStatus() != PostStatus.DRAFT) {
            throw new PostStatusNotAllowedException(draftId, draft.getStatus(), "publish");
        }
        
        // Validate content before publishing
        if (!StringUtils.hasText(draft.getTitle())) {
            throw new SensitiveContentException("title", "Title cannot be empty");
        }
        
        // Check for sensitive content
        checkSensitiveContent(draft.getTitle(), draft.getContent());
        
        // Publish the draft
        draft.setStatus(PostStatus.PUBLISHED);
        Post publishedPost = postRepository.save(draft);
        
        // Create public metrics
        PublicMatric publicMatric = new PublicMatric();
        publicMatric.setPost(publishedPost);
        publicMatric.setComments(0);
        publicMatric.setLikes(0);
        publicMatric.setFavorites(0);
        publicMatricRepository.save(publicMatric);
        
        log.info("Published post {} for user {}", publishedPost.getId(), userId);
        
        return publishedPost;
    }

    @Override
    public Page<Post> getPublishedPostsByUser(Integer userId, Pageable pageable) {
        log.debug("Getting published posts for user: {}", userId);
        return postRepository.findByAuthorIdAndStatus(userId, PostStatus.PUBLISHED, pageable);
    }

    @Override
    public Page<Post> getAllPublishedPostsByCategory(String category, Pageable pageable) {
        log.debug("Getting published posts by category: {}", category);
        if (StringUtils.hasText(category)) {
            return postRepository.findByStatusAndCategory(PostStatus.PUBLISHED, category, pageable);
        } else {
            return postRepository.findByStatus(PostStatus.PUBLISHED, pageable);
        }
    }

    @Override
    public Post getPublishedPostById(Integer postId) {
        log.debug("Getting published post by id: {}", postId);
        return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED).orElse(null);
    }

    @Override
    public Page<Post> searchPublishedPosts(String keyword, Pageable pageable) {
        log.debug("Searching published posts with keyword: {}", keyword);
        if (!StringUtils.hasText(keyword)) {
            return postRepository.findByStatus(PostStatus.PUBLISHED, pageable);
        }
        return postRepository.findByStatusAndTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                PostStatus.PUBLISHED, keyword, keyword, pageable);
    }

    @Override
    public Page<Post> getHotPublishedPosts(Pageable pageable) {
        log.debug("Getting hot published posts");
        // Sort by creation time descending for now - in production, use more sophisticated algorithm
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return postRepository.findByStatus(PostStatus.PUBLISHED, sortedPageable);
    }

    @Override
    @Transactional
    public Post updatePostStatus(Integer postId, PostStatus newStatus) throws PostNotFoundException {
        log.debug("Updating post {} status to {}", postId, newStatus);
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        
        post.setStatus(newStatus);
        Post updatedPost = postRepository.save(post);
        
        log.info("Updated post {} status to {}", postId, newStatus);
        return updatedPost;
    }

    @Override
    @Transactional
    public Boolean deletePost(Integer userId, Integer postId) 
            throws PostNotFoundException, ModifyNotPermittedException, DeletionFailedException {
        
        log.debug("Deleting post {} for user {}", postId, userId);
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));
        
        // Check ownership
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ModifyNotPermittedException(postId, userId, "User does not own post");
        }
        
        try {
            postRepository.delete(post);
            log.info("Deleted post {} for user {}", postId, userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete post {} for user {}: {}", postId, userId, e.getMessage());
            throw new DeletionFailedException(postId, "post", e.getMessage(), e);
        }
    }

    private void checkSensitiveContent(String title, String content) throws SensitiveContentException {
        // Simple keyword-based filtering - in production, use ML/AI-based detection
        String textToCheck = (title + " " + (content != null ? content : "")).toLowerCase();
        
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (textToCheck.contains(keyword.toLowerCase())) {
                throw new SensitiveContentException("content", "Contains prohibited keyword: " + keyword);
            }
        }
    }
}