package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.model.BindType;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.model.UserBinder;
import edu.xtu.bbs.user.repo.UserBinderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserBinderService {

    private final UserBinderRepository userBinderRepository;

    public UserBinderService(UserBinderRepository userBinderRepository) {
        this.userBinderRepository = userBinderRepository;
    }

    /**
     * 为用户绑定邮箱
     */
    public UserBinder bindEmail(User user, String email) throws EmailAlreadyExistsException {
        return bindIdentifier(user.getId(), BindType.EMAIL, email);
    }

    /**
     * 为用户绑定标识符
     */
    public UserBinder bindIdentifier(Integer userId, BindType bindType, String identifier) {
        // 检查是否已经被其他用户绑定
        if (userBinderRepository.existsByIdentifierAndBindType(identifier, bindType)) {
            throw new RuntimeException("Identifier " + identifier + " of type " + bindType + " is already bound to another user");
        }

        // 检查用户是否已经绑定了该类型
        Optional<UserBinder> existing = userBinderRepository.findByUserIdAndBindType(userId, bindType);

        UserBinder userBinder;
        if (existing.isPresent()) {
            // 更新现有绑定
            userBinder = existing.get();
            userBinder.setIdentifier(identifier);
        } else {
            // 创建新绑定
            userBinder = new UserBinder();
            userBinder.setUserId(userId);
            userBinder.setBindType(bindType);
            userBinder.setIdentifier(identifier);
        }

        return userBinderRepository.save(userBinder);
    }

    /**
     * 根据邮箱查找用户ID
     */
    @Transactional(readOnly = true)
    public Optional<Integer> findUserIdByEmail(String email) {
        return userBinderRepository.findByIdentifierAndBindType(email, BindType.EMAIL)
                .map(UserBinder::getUserId);
    }

    /**
     * 根据用户ID查找邮箱
     */
    @Transactional(readOnly = true)
    public Optional<String> findEmailByUserId(Integer userId) {
        return userBinderRepository.findByUserIdAndBindType(userId, BindType.EMAIL)
                .map(UserBinder::getIdentifier);
    }

    /**
     * 检查邮箱是否已存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userBinderRepository.existsByIdentifierAndBindType(email, BindType.EMAIL);
    }

    /**
     * 检查用户是否已绑定邮箱
     */
    @Transactional(readOnly = true)
    public boolean userHasEmail(Integer userId) {
        return userBinderRepository.existsByUserIdAndBindType(userId, BindType.EMAIL);
    }

    /**
     * 获取用户的所有绑定信息
     */
    @Transactional(readOnly = true)
    public List<UserBinder> getUserBinders(Integer userId) {
        return userBinderRepository.findByUserId(userId);
    }

    /**
     * 删除用户的邮箱绑定
     */
    public boolean unbindEmail(Integer userId) {
        int deleted = userBinderRepository.deleteByUserIdAndBindType(userId, BindType.EMAIL);
        return deleted > 0;
    }

    /**
     * 更新用户的邮箱绑定
     */
    public boolean updateEmail(Integer userId, String newEmail) {
        // 检查新邮箱是否已被其他用户绑定
        if (userBinderRepository.existsByIdentifierAndBindType(newEmail, BindType.EMAIL)) {
            Optional<UserBinder> existingBinder = userBinderRepository.findByIdentifierAndBindType(newEmail, BindType.EMAIL);
            if (existingBinder.isPresent() && !existingBinder.get().getUserId().equals(userId)) {
                throw new RuntimeException("Email " + newEmail + " is already bound to another user");
            }
        }

        int updated = userBinderRepository.updateIdentifierByUserIdAndBindType(newEmail, userId, BindType.EMAIL);
        return updated > 0;
    }
}