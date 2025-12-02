package edu.xtu.bbs.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.common.validation.SensitiveWordsDetector;
import edu.xtu.bbs.post.service.PostService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PostController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = edu.xtu.bbs.user.filter.JwtAuthorizationFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "edu\\.xtu\\.bbs\\.user\\.filter\\..*")
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private SensitiveWordsDetector sensitiveWordsDetector;

    @Test
    @DisplayName("createPost should reject content flagged by ContentAudit")
    void createPost_ShouldReturnBadRequest_WhenContentSensitive() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(buildUser(7));
        when(sensitiveWordsDetector.containsSensitiveWords("Sensitive payload"))
                .thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(new TestCreatePostRequest(
                "Clean Title",
                "Sensitive payload",
                "general"
        ));

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.PARAM_INVALID.getMessage()))
                .andExpect(jsonPath("$.data.content").value("Content contains prohibited expressions."));

        verify(postService, never()).createPost(anyInt(), any());
    }

    private static User buildUser(int id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private record TestCreatePostRequest(String title, String content, String category) {
    }
}
