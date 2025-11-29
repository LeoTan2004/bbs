package edu.xtu.bbs.post.service.impl;

import edu.xtu.bbs.post.exception.CommentNotFoundException;
import edu.xtu.bbs.post.model.CommentLike;
import edu.xtu.bbs.post.model.PostComment;
import edu.xtu.bbs.post.repo.CommentLikeRepository;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论点赞服务实现
 * 
 * @author BBS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    public boolean hasLikedComment(Integer userId, Integer commentId) {
        log.debug("Checking if user {} has liked comment {}", userId, commentId);
        
        if (userId == null || commentId == null) {
            return false;
        }
        
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    @Override
    @Transactional
    public void likeComment(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException, IllegalStateException {
        
        log.debug("User {} liking comment {}", userId, commentId);
        
        // 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        // 验证评论存在
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        // 检查是否已经点赞
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new IllegalStateException("User has already liked this comment");
        }
        
        // Create like record
        CommentLike commentLike = new CommentLike();
        commentLike.setComment(comment);
        commentLike.setUser(user);
        commentLikeRepository.save(commentLike);
        
        // 更新评论的点赞数量
        comment.setLikes(comment.getLikes() + 1);
        commentRepository.save(comment);
        
        log.info("User {} liked comment {}", userId, commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException, IllegalStateException {
        
        log.debug("User {} unliking comment {}", userId, commentId);
        
        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        
        // 验证评论存在
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        // 检查是否已经点赞
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new IllegalStateException("User has not liked this comment");
        }
        
        // Delete like record
        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        
        // 更新评论的点赞数量
        comment.setLikes(Math.max(0, comment.getLikes() - 1));
        commentRepository.save(comment);
        
        log.info("User {} unliked comment {}", userId, commentId);
    }

    @Override
    public long getCommentLikeCount(Integer commentId) {
        if (commentId == null) {
            return 0;
        }
        return commentLikeRepository.countByCommentId(commentId);
    }

    @Override
    @Transactional
    public boolean toggleCommentLike(Integer userId, Integer commentId) 
            throws UserNotFoundException, CommentNotFoundException {
        
        log.debug("User {} toggling like for comment {}", userId, commentId);
        
        boolean isCurrentlyLiked = hasLikedComment(userId, commentId);
        
        if (isCurrentlyLiked) {
            unlikeComment(userId, commentId);
            return false;
        } else {
            likeComment(userId, commentId);
            return true;
        }
    }
}