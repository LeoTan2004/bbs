package edu.xtu.bbs.post.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.post.dto.*;
import edu.xtu.bbs.post.model.*;
import edu.xtu.bbs.post.service.PostCommentMediumService;
import edu.xtu.bbs.post.service.PostCommentService;
import edu.xtu.bbs.post.service.PostMediumService;
import edu.xtu.bbs.post.service.PostService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Post and Comment Flow Integration Test")
class PostAndCommentFlowTest {

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostMediumService postMediumService;

    @MockitoBean
    private PostCommentService postCommentService;

    @MockitoBean
    private PostCommentMediumService postCommentMediumService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setNickname("Test User");
        mockUser.setPasswordHash("hashedPassword");
    }

    private Post newDraft(Integer id) {
        Post p = new Post();
        p.setId(id);
        p.setAuthor(mockUser);
        p.setTitle("Draft Title");
        p.setContent("Initial draft content");
        p.setStatus(PostStatus.DRAFT);
        return p;
    }

    private Post published(Integer id) {
        Post p = new Post();
        p.setId(id);
        p.setAuthor(mockUser);
        p.setTitle("Published Title");
        p.setContent("Published content");
        p.setStatus(PostStatus.PUBLISHED);
        return p;
    }

    private Medium medium(String id, String type, String accessUrl) {
        Medium m = new Medium();
        m.setId(id);
        m.setType(type);
        m.setDisplayUrl(accessUrl);
        m.setResourceUrl(accessUrl);
        return m;
    }

    private PostComment newComment(Integer id, Integer postId) {
        PostComment c = new PostComment();
        c.setId(id);
        Post post = published(postId);
        c.setPost(post);
        c.setUser(mockUser);
        c.setContent("Nice post!");
        c.setStatus(CommentStatus.PUBLISHED);
        return c;
    }

    @Test
    @DisplayName("New Post and Comment Flow")
    void testCreateEditPublishDraft() throws Exception {
        // GET /posts/draft
        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(postService.getDraft(mockUser.getId())).thenReturn(newDraft(11));

        mockMvc.perform(get("/posts/draft").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // PUT /posts/draft/{id}
        DraftContentEditor editor = new DraftContentEditor("Updated Title", "Updated draft content", null);
        String editorJson = objectMapper.writeValueAsString(editor);

        Post edited = newDraft(11);
        edited.setTitle("Updated Title");
        edited.setContent("Updated draft content");
        when(postService.editDraft(eq(mockUser.getId()), eq(11), any(DraftContentEditor.class))).thenReturn(edited);

        mockMvc.perform(put("/posts/draft/{draftId}", 11)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editorJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.content").value("Updated draft content"));

        // POST /posts/draft/{id}/publish
        when(postService.publishDraft(eq(mockUser.getId()), eq(11))).thenReturn(published(11));

        mockMvc.perform(post("/posts/draft/{draftId}/publish", 11).with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Upload Media to Post")
    void testUploadPostImage() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        PostMediumUploadRequest req = new PostMediumUploadRequest("image/jpeg", 1024L);
        String body = objectMapper.writeValueAsString(req);

        MediumUploadResult result = new MediumUploadResult(
                "https://cos.example.com/upload/post/11/img-1.jpg",
                "https://cos.example.com/post/11/img-1.jpg"
        );
        when(postMediumService.uploadMediumToDraft(eq(mockUser.getId()), eq(11), any(PostMediumUploadRequest.class)))
                .thenReturn(result);

        mockMvc.perform(post("/posts/{postId}/media", 11)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://cos.example.com/upload/post/11/img-1.jpg"))
                .andExpect(jsonPath("$.data.accessUrl").value("https://cos.example.com/post/11/img-1.jpg"));
    }

    @Test
    @DisplayName("View Published Post")
    void testViewPublishedPost() throws Exception {
        when(postService.getPublishedPostById(11)).thenReturn(published(11));

        mockMvc.perform(get("/posts/{postId}", 11).with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Published Post with Media")
    void testCreateCommentWithImage() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(mockUser);

        // Create comment
        CreateCommentRequest createReq = new CreateCommentRequest(11, "Nice post!", null);
        String createJson = objectMapper.writeValueAsString(createReq);

        PostComment created = newComment(101, 11);
        when(postCommentService.createComment(eq(mockUser.getId()), any(CreateCommentRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/comments")
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.id").value(101));

        // Upload image to comment
        CommentMediumUploadRequest cmReq = new CommentMediumUploadRequest("image/png", 2048L);
        String cmJson = objectMapper.writeValueAsString(cmReq);

        MediumUploadResult cmResult = new MediumUploadResult(
                "https://cos.example.com/upload/comment/101/cimg-1.png",
                "https://cos.example.com/comment/101/cimg-1.png"
        );
        when(postCommentMediumService.uploadMediumToComment(eq(mockUser.getId()), eq(101), any(CommentMediumUploadRequest.class)))
                .thenReturn(cmResult);

        mockMvc.perform(post("/comments/{commentId}/media", 101)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cmJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://cos.example.com/upload/comment/101/cimg-1.png"))
                .andExpect(jsonPath("$.data.accessUrl").value("https://cos.example.com/comment/101/cimg-1.png"));
    }
}
