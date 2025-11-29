package edu.xtu.bbs.post.integration;

import edu.xtu.bbs.post.repo.CommentLikeRepository;
import edu.xtu.bbs.post.repo.PostCommentRepository;
import edu.xtu.bbs.post.service.CommentLikeService;
import edu.xtu.bbs.post.service.impl.CommentLikeServiceImpl;
import edu.xtu.bbs.user.repo.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CommentLikeIntegrationTestConfig {

    @Bean
    public CommentLikeService commentLikeService(
            CommentLikeRepository commentLikeRepository,
            PostCommentRepository commentRepository,
            UserRepository userRepository) {
        return new CommentLikeServiceImpl(commentLikeRepository, commentRepository, userRepository);
    }
}