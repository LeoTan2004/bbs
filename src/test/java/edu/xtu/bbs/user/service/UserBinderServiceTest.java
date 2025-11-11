package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.model.BindType;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.model.UserBinder;
import edu.xtu.bbs.user.repo.UserBinderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBinderService Tests")
class UserBinderServiceTest {

    @Mock
    private UserBinderRepository userBinderRepository;

    @InjectMocks
    private UserBinderService userBinderService;

    private User testUser;
    private UserBinder testUserBinder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");

        testUserBinder = new UserBinder();
        testUserBinder.setId(1);
        testUserBinder.setUserId(1);
        testUserBinder.setBindType(BindType.EMAIL);
        testUserBinder.setIdentifier("test@example.com");
        testUserBinder.setCreatedAt(Instant.now());
        testUserBinder.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should bind email successfully when email does not exist")
    void bindEmail_WhenEmailDoesNotExist_ShouldReturnUserBinder() throws EmailAlreadyExistsException {
        // Given
        String email = "test@example.com";
        when(userBinderRepository.existsByIdentifierAndBindType(email, BindType.EMAIL)).thenReturn(false);
        when(userBinderRepository.findByUserIdAndBindType(testUser.getId(), BindType.EMAIL)).thenReturn(Optional.empty());
        when(userBinderRepository.save(isA(UserBinder.class))).thenReturn(testUserBinder);

        // When
        UserBinder result = userBinderService.bindEmail(testUser, email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getBindType()).isEqualTo(BindType.EMAIL);
        assertThat(result.getIdentifier()).isEqualTo(email);

        verify(userBinderRepository).existsByIdentifierAndBindType(email, BindType.EMAIL);
        verify(userBinderRepository).findByUserIdAndBindType(testUser.getId(), BindType.EMAIL);
        verify(userBinderRepository).save(isA(UserBinder.class));
    }

    @Test
    @DisplayName("Should bind identifier successfully when identifier does not exist")
    void bindIdentifier_WhenIdentifierDoesNotExist_ShouldReturnUserBinder() {
        // Given
        Integer userId = 1;
        BindType bindType = BindType.EMAIL;
        String identifier = "test@example.com";

        when(userBinderRepository.existsByIdentifierAndBindType(identifier, bindType)).thenReturn(false);
        when(userBinderRepository.findByUserIdAndBindType(userId, bindType)).thenReturn(Optional.empty());
        when(userBinderRepository.save(isA(UserBinder.class))).thenReturn(testUserBinder);

        // When
        UserBinder result = userBinderService.bindIdentifier(userId, bindType, identifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBindType()).isEqualTo(bindType);
        assertThat(result.getIdentifier()).isEqualTo(identifier);

        verify(userBinderRepository).existsByIdentifierAndBindType(identifier, bindType);
        verify(userBinderRepository).findByUserIdAndBindType(userId, bindType);
        verify(userBinderRepository).save(isA(UserBinder.class));
    }

    @Test
    @DisplayName("Should update existing binding when user already has binding of same type")
    void bindIdentifier_WhenUserAlreadyHasBindingOfSameType_ShouldUpdateExisting() {
        // Given
        Integer userId = 1;
        BindType bindType = BindType.EMAIL;
        String newIdentifier = "newemail@example.com";

        UserBinder existingBinder = new UserBinder();
        existingBinder.setId(1);
        existingBinder.setUserId(userId);
        existingBinder.setBindType(bindType);
        existingBinder.setIdentifier("oldemail@example.com");

        when(userBinderRepository.existsByIdentifierAndBindType(newIdentifier, bindType)).thenReturn(false);
        when(userBinderRepository.findByUserIdAndBindType(userId, bindType)).thenReturn(Optional.of(existingBinder));
        when(userBinderRepository.save(isA(UserBinder.class))).thenReturn(existingBinder);

        // When
        UserBinder result = userBinderService.bindIdentifier(userId, bindType, newIdentifier);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdentifier()).isEqualTo(newIdentifier);

        verify(userBinderRepository).existsByIdentifierAndBindType(newIdentifier, bindType);
        verify(userBinderRepository).findByUserIdAndBindType(userId, bindType);
        verify(userBinderRepository).save(existingBinder);
    }

    @Test
    @DisplayName("Should throw RuntimeException when identifier already bound by another user")
    void bindIdentifier_WhenIdentifierAlreadyBound_ShouldThrowRuntimeException() {
        // Given
        Integer userId = 1;
        BindType bindType = BindType.EMAIL;
        String identifier = "test@example.com";

        when(userBinderRepository.existsByIdentifierAndBindType(identifier, bindType)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userBinderService.bindIdentifier(userId, bindType, identifier))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Identifier " + identifier + " of type " + bindType + " is already bound to another user");

        verify(userBinderRepository).existsByIdentifierAndBindType(identifier, bindType);
        verify(userBinderRepository, never()).save(isA(UserBinder.class));
    }

    @Test
    @DisplayName("Should find user ID by email successfully")
    void findUserIdByEmail_WhenEmailExists_ShouldReturnUserId() {
        // Given
        String email = "test@example.com";
        when(userBinderRepository.findByIdentifierAndBindType(email, BindType.EMAIL))
                .thenReturn(Optional.of(testUserBinder));

        // When
        Optional<Integer> result = userBinderService.findUserIdByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserBinder.getUserId());

        verify(userBinderRepository).findByIdentifierAndBindType(email, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findUserIdByEmail_WhenEmailDoesNotExist_ShouldReturnEmpty() {
        // Given
        String email = "nonexistent@example.com";
        when(userBinderRepository.findByIdentifierAndBindType(email, BindType.EMAIL))
                .thenReturn(Optional.empty());

        // When
        Optional<Integer> result = userBinderService.findUserIdByEmail(email);

        // Then
        assertThat(result).isEmpty();

        verify(userBinderRepository).findByIdentifierAndBindType(email, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should find email by user ID successfully")
    void findEmailByUserId_WhenEmailExists_ShouldReturnEmail() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.findByUserIdAndBindType(userId, BindType.EMAIL))
                .thenReturn(Optional.of(testUserBinder));

        // When
        Optional<String> result = userBinderService.findEmailByUserId(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserBinder.getIdentifier());

        verify(userBinderRepository).findByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return empty when user has no email binding")
    void findEmailByUserId_WhenUserHasNoEmail_ShouldReturnEmpty() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.findByUserIdAndBindType(userId, BindType.EMAIL))
                .thenReturn(Optional.empty());

        // When
        Optional<String> result = userBinderService.findEmailByUserId(userId);

        // Then
        assertThat(result).isEmpty();

        verify(userBinderRepository).findByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        when(userBinderRepository.existsByIdentifierAndBindType(email, BindType.EMAIL)).thenReturn(true);

        // When
        boolean result = userBinderService.existsByEmail(email);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByIdentifierAndBindType(email, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // Given
        String email = "nonexistent@example.com";
        when(userBinderRepository.existsByIdentifierAndBindType(email, BindType.EMAIL)).thenReturn(false);

        // When
        boolean result = userBinderService.existsByEmail(email);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(email, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return true when user has email binding")
    void userHasEmail_WhenUserHasEmail_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.existsByUserIdAndBindType(userId, BindType.EMAIL)).thenReturn(true);

        // When
        boolean result = userBinderService.userHasEmail(userId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return false when user has no email binding")
    void userHasEmail_WhenUserHasNoEmail_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.existsByUserIdAndBindType(userId, BindType.EMAIL)).thenReturn(false);

        // When
        boolean result = userBinderService.userHasEmail(userId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return all user binders for user")
    void getUserBinders_ShouldReturnAllBindersForUser() {
        // Given
        Integer userId = 1;
        UserBinder emailBinder = new UserBinder();
        emailBinder.setUserId(userId);
        emailBinder.setBindType(BindType.EMAIL);
        emailBinder.setIdentifier("test@example.com");

        UserBinder wechatBinder = new UserBinder();
        wechatBinder.setUserId(userId);
        wechatBinder.setBindType(BindType.WECHAT);
        wechatBinder.setIdentifier("wechat_user");

        List<UserBinder> userBinders = Arrays.asList(emailBinder, wechatBinder);
        when(userBinderRepository.findByUserId(userId)).thenReturn(userBinders);

        // When
        List<UserBinder> result = userBinderService.getUserBinders(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(userBinders);

        verify(userBinderRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should unbind email successfully when user has email binding")
    void unbindEmail_WhenUserHasEmail_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.deleteByUserIdAndBindType(userId, BindType.EMAIL)).thenReturn(1);

        // When
        boolean result = userBinderService.unbindEmail(userId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).deleteByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should return false when user has no email binding to unbind")
    void unbindEmail_WhenUserHasNoEmail_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.deleteByUserIdAndBindType(userId, BindType.EMAIL)).thenReturn(0);

        // When
        boolean result = userBinderService.unbindEmail(userId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).deleteByUserIdAndBindType(userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should update email successfully when new email is not bound by another user")
    void updateEmail_WhenNewEmailNotBoundByAnother_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        String newEmail = "newemail@example.com";

        when(userBinderRepository.existsByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(false);
        when(userBinderRepository.updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL)).thenReturn(1);

        // When
        boolean result = userBinderService.updateEmail(userId, newEmail);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verify(userBinderRepository).updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should update email successfully when new email is bound by same user")
    void updateEmail_WhenNewEmailBoundBySameUser_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        String newEmail = "newemail@example.com";

        UserBinder existingBinder = new UserBinder();
        existingBinder.setUserId(userId);
        existingBinder.setBindType(BindType.EMAIL);
        existingBinder.setIdentifier(newEmail);

        when(userBinderRepository.existsByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(true);
        when(userBinderRepository.findByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(Optional.of(existingBinder));
        when(userBinderRepository.updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL)).thenReturn(1);

        // When
        boolean result = userBinderService.updateEmail(userId, newEmail);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verify(userBinderRepository).findByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verify(userBinderRepository).updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should throw RuntimeException when new email is bound by another user")
    void updateEmail_WhenNewEmailBoundByAnotherUser_ShouldThrowRuntimeException() {
        // Given
        Integer userId = 1;
        String newEmail = "newemail@example.com";

        UserBinder existingBinder = new UserBinder();
        existingBinder.setUserId(2); // Different user
        existingBinder.setBindType(BindType.EMAIL);
        existingBinder.setIdentifier(newEmail);

        when(userBinderRepository.existsByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(true);
        when(userBinderRepository.findByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(Optional.of(existingBinder));

        // When & Then
        assertThatThrownBy(() -> userBinderService.updateEmail(userId, newEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email " + newEmail + " is already bound to another user");

        verify(userBinderRepository).existsByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verify(userBinderRepository).findByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verifyNoMoreInteractions(userBinderRepository);
    }

    @Test
    @DisplayName("Should return false when no binding to update")
    void updateEmail_WhenNoBindingToUpdate_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        String newEmail = "newemail@example.com";

        when(userBinderRepository.existsByIdentifierAndBindType(newEmail, BindType.EMAIL)).thenReturn(false);
        when(userBinderRepository.updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL)).thenReturn(0);

        // When
        boolean result = userBinderService.updateEmail(userId, newEmail);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(newEmail, BindType.EMAIL);
        verify(userBinderRepository).updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should handle null email gracefully in existsByEmail")
    void existsByEmail_WithNullEmail_ShouldReturnFalse() {
        // Given
        when(userBinderRepository.existsByIdentifierAndBindType(null, BindType.EMAIL)).thenReturn(false);

        // When
        boolean result = userBinderService.existsByEmail(null);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(null, BindType.EMAIL);
    }

    @Test
    @DisplayName("Should handle null user ID gracefully in userHasEmail")
    void userHasEmail_WithNullUserId_ShouldReturnFalse() {
        // Given
        when(userBinderRepository.existsByUserIdAndBindType(null, BindType.EMAIL)).thenReturn(false);

        // When
        boolean result = userBinderService.userHasEmail(null);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByUserIdAndBindType(null, BindType.EMAIL);
    }

    // WeChat related tests

    @Test
    @DisplayName("Should bind WeChat successfully with User object")
    void bindWeChat_WithUser_WhenOpenIdDoesNotExist_ShouldReturnUserBinder() {
        // Given
        String openId = "wechat_openid_123";
        UserBinder weChatBinder = new UserBinder();
        weChatBinder.setId(2);
        weChatBinder.setUserId(testUser.getId());
        weChatBinder.setBindType(BindType.WECHAT);
        weChatBinder.setIdentifier(openId);

        when(userBinderRepository.existsByIdentifierAndBindType(openId, BindType.WECHAT)).thenReturn(false);
        when(userBinderRepository.findByUserIdAndBindType(testUser.getId(), BindType.WECHAT)).thenReturn(Optional.empty());
        when(userBinderRepository.save(isA(UserBinder.class))).thenReturn(weChatBinder);

        // When
        UserBinder result = userBinderService.bindWeChat(testUser, openId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getBindType()).isEqualTo(BindType.WECHAT);
        assertThat(result.getIdentifier()).isEqualTo(openId);

        verify(userBinderRepository).existsByIdentifierAndBindType(openId, BindType.WECHAT);
        verify(userBinderRepository).findByUserIdAndBindType(testUser.getId(), BindType.WECHAT);
        verify(userBinderRepository).save(isA(UserBinder.class));
    }

    @Test
    @DisplayName("Should bind WeChat successfully with user ID")
    void bindWeChat_WithUserId_WhenOpenIdDoesNotExist_ShouldReturnUserBinder() {
        // Given
        Integer userId = 1;
        String openId = "wechat_openid_123";
        UserBinder weChatBinder = new UserBinder();
        weChatBinder.setId(2);
        weChatBinder.setUserId(userId);
        weChatBinder.setBindType(BindType.WECHAT);
        weChatBinder.setIdentifier(openId);

        when(userBinderRepository.existsByIdentifierAndBindType(openId, BindType.WECHAT)).thenReturn(false);
        when(userBinderRepository.findByUserIdAndBindType(userId, BindType.WECHAT)).thenReturn(Optional.empty());
        when(userBinderRepository.save(isA(UserBinder.class))).thenReturn(weChatBinder);

        // When
        UserBinder result = userBinderService.bindWeChat(userId, openId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBindType()).isEqualTo(BindType.WECHAT);
        assertThat(result.getIdentifier()).isEqualTo(openId);

        verify(userBinderRepository).existsByIdentifierAndBindType(openId, BindType.WECHAT);
        verify(userBinderRepository).findByUserIdAndBindType(userId, BindType.WECHAT);
        verify(userBinderRepository).save(isA(UserBinder.class));
    }

    @Test
    @DisplayName("Should throw exception when WeChat openId already exists")
    void bindWeChat_WhenOpenIdExists_ShouldThrowException() {
        // Given
        String openId = "existing_wechat_openid";
        when(userBinderRepository.existsByIdentifierAndBindType(openId, BindType.WECHAT)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userBinderService.bindWeChat(testUser, openId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Identifier " + openId + " of type " + BindType.WECHAT + " is already bound to another user");

        verify(userBinderRepository).existsByIdentifierAndBindType(openId, BindType.WECHAT);
        verifyNoMoreInteractions(userBinderRepository);
    }

    @Test
    @DisplayName("Should find user ID by WeChat openId")
    void findUserIdByWeChatOpenId_WhenOpenIdExists_ShouldReturnUserId() {
        // Given
        String openId = "wechat_openid_123";
        Integer expectedUserId = 1;
        UserBinder weChatBinder = new UserBinder();
        weChatBinder.setUserId(expectedUserId);
        weChatBinder.setBindType(BindType.WECHAT);
        weChatBinder.setIdentifier(openId);

        when(userBinderRepository.findByIdentifierAndBindType(openId, BindType.WECHAT))
                .thenReturn(Optional.of(weChatBinder));

        // When
        Optional<Integer> result = userBinderService.findUserIdByWeChatOpenId(openId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedUserId);

        verify(userBinderRepository).findByIdentifierAndBindType(openId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return empty when WeChat openId does not exist")
    void findUserIdByWeChatOpenId_WhenOpenIdDoesNotExist_ShouldReturnEmpty() {
        // Given
        String openId = "nonexistent_wechat_openid";
        when(userBinderRepository.findByIdentifierAndBindType(openId, BindType.WECHAT))
                .thenReturn(Optional.empty());

        // When
        Optional<Integer> result = userBinderService.findUserIdByWeChatOpenId(openId);

        // Then
        assertThat(result).isEmpty();

        verify(userBinderRepository).findByIdentifierAndBindType(openId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should find WeChat openId by user ID")
    void findWeChatOpenIdByUserId_WhenUserHasWeChatBinding_ShouldReturnOpenId() {
        // Given
        Integer userId = 1;
        String expectedOpenId = "wechat_openid_123";
        UserBinder weChatBinder = new UserBinder();
        weChatBinder.setUserId(userId);
        weChatBinder.setBindType(BindType.WECHAT);
        weChatBinder.setIdentifier(expectedOpenId);

        when(userBinderRepository.findByUserIdAndBindType(userId, BindType.WECHAT))
                .thenReturn(Optional.of(weChatBinder));

        // When
        Optional<String> result = userBinderService.findWeChatOpenIdByUserId(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedOpenId);

        verify(userBinderRepository).findByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return empty when user has no WeChat binding")
    void findWeChatOpenIdByUserId_WhenUserHasNoWeChatBinding_ShouldReturnEmpty() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.findByUserIdAndBindType(userId, BindType.WECHAT))
                .thenReturn(Optional.empty());

        // When
        Optional<String> result = userBinderService.findWeChatOpenIdByUserId(userId);

        // Then
        assertThat(result).isEmpty();

        verify(userBinderRepository).findByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return true when WeChat openId exists")
    void existsByWeChatOpenId_WhenOpenIdExists_ShouldReturnTrue() {
        // Given
        String openId = "wechat_openid_123";
        when(userBinderRepository.existsByIdentifierAndBindType(openId, BindType.WECHAT)).thenReturn(true);

        // When
        boolean result = userBinderService.existsByWeChatOpenId(openId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByIdentifierAndBindType(openId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return false when WeChat openId does not exist")
    void existsByWeChatOpenId_WhenOpenIdDoesNotExist_ShouldReturnFalse() {
        // Given
        String openId = "nonexistent_wechat_openid";
        when(userBinderRepository.existsByIdentifierAndBindType(openId, BindType.WECHAT)).thenReturn(false);

        // When
        boolean result = userBinderService.existsByWeChatOpenId(openId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(openId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return true when user has WeChat binding")
    void userHasWeChatBinding_WhenUserHasWeChat_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.existsByUserIdAndBindType(userId, BindType.WECHAT)).thenReturn(true);

        // When
        boolean result = userBinderService.userHasWeChatBinding(userId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return false when user has no WeChat binding")
    void userHasWeChatBinding_WhenUserHasNoWeChat_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.existsByUserIdAndBindType(userId, BindType.WECHAT)).thenReturn(false);

        // When
        boolean result = userBinderService.userHasWeChatBinding(userId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should unbind WeChat successfully")
    void unbindWeChat_WhenWeChatBindingExists_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.deleteByUserIdAndBindType(userId, BindType.WECHAT)).thenReturn(1);

        // When
        boolean result = userBinderService.unbindWeChat(userId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).deleteByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should return false when no WeChat binding to unbind")
    void unbindWeChat_WhenNoWeChatBinding_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        when(userBinderRepository.deleteByUserIdAndBindType(userId, BindType.WECHAT)).thenReturn(0);

        // When
        boolean result = userBinderService.unbindWeChat(userId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).deleteByUserIdAndBindType(userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should update WeChat openId successfully")
    void updateWeChatOpenId_WhenNewOpenIdDoesNotExist_ShouldReturnTrue() {
        // Given
        Integer userId = 1;
        String newOpenId = "new_wechat_openid";

        when(userBinderRepository.existsByIdentifierAndBindType(newOpenId, BindType.WECHAT)).thenReturn(false);
        when(userBinderRepository.updateIdentifierByUserIdAndBindType(newOpenId, userId, BindType.WECHAT)).thenReturn(1);

        // When
        boolean result = userBinderService.updateWeChatOpenId(userId, newOpenId);

        // Then
        assertThat(result).isTrue();

        verify(userBinderRepository).existsByIdentifierAndBindType(newOpenId, BindType.WECHAT);
        verify(userBinderRepository).updateIdentifierByUserIdAndBindType(newOpenId, userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should throw exception when trying to update to existing WeChat openId of another user")
    void updateWeChatOpenId_WhenNewOpenIdExistsForAnotherUser_ShouldThrowException() {
        // Given
        Integer userId = 1;
        String newOpenId = "existing_wechat_openid";
        UserBinder existingBinder = new UserBinder();
        existingBinder.setUserId(2); // Different user
        existingBinder.setBindType(BindType.WECHAT);
        existingBinder.setIdentifier(newOpenId);

        when(userBinderRepository.existsByIdentifierAndBindType(newOpenId, BindType.WECHAT)).thenReturn(true);
        when(userBinderRepository.findByIdentifierAndBindType(newOpenId, BindType.WECHAT)).thenReturn(Optional.of(existingBinder));

        // When & Then
        assertThatThrownBy(() -> userBinderService.updateWeChatOpenId(userId, newOpenId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("WeChat OpenID " + newOpenId + " is already bound to another user");

        verify(userBinderRepository).existsByIdentifierAndBindType(newOpenId, BindType.WECHAT);
        verify(userBinderRepository).findByIdentifierAndBindType(newOpenId, BindType.WECHAT);
        verifyNoMoreInteractions(userBinderRepository);
    }

    @Test
    @DisplayName("Should return false when no WeChat binding to update")
    void updateWeChatOpenId_WhenNoBindingToUpdate_ShouldReturnFalse() {
        // Given
        Integer userId = 1;
        String newOpenId = "new_wechat_openid";

        when(userBinderRepository.existsByIdentifierAndBindType(newOpenId, BindType.WECHAT)).thenReturn(false);
        when(userBinderRepository.updateIdentifierByUserIdAndBindType(newOpenId, userId, BindType.WECHAT)).thenReturn(0);

        // When
        boolean result = userBinderService.updateWeChatOpenId(userId, newOpenId);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(newOpenId, BindType.WECHAT);
        verify(userBinderRepository).updateIdentifierByUserIdAndBindType(newOpenId, userId, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should handle null WeChat openId gracefully in existsByWeChatOpenId")
    void existsByWeChatOpenId_WithNullOpenId_ShouldReturnFalse() {
        // Given
        when(userBinderRepository.existsByIdentifierAndBindType(null, BindType.WECHAT)).thenReturn(false);

        // When
        boolean result = userBinderService.existsByWeChatOpenId(null);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByIdentifierAndBindType(null, BindType.WECHAT);
    }

    @Test
    @DisplayName("Should handle null user ID gracefully in userHasWeChatBinding")
    void userHasWeChatBinding_WithNullUserId_ShouldReturnFalse() {
        // Given
        when(userBinderRepository.existsByUserIdAndBindType(null, BindType.WECHAT)).thenReturn(false);

        // When
        boolean result = userBinderService.userHasWeChatBinding(null);

        // Then
        assertThat(result).isFalse();

        verify(userBinderRepository).existsByUserIdAndBindType(null, BindType.WECHAT);
    }
}