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
     * Bind email for user
     */
    public UserBinder bindEmail(User user, String email) throws EmailAlreadyExistsException {
        return bindIdentifier(user.getId(), BindType.EMAIL, email);
    }

    /**
     * Bind identifier for user
     */
    public UserBinder bindIdentifier(Integer userId, BindType bindType, String identifier) {
        // Check if already bound by another user
        if (userBinderRepository.existsByIdentifierAndBindType(identifier, bindType)) {
            throw new RuntimeException("Identifier " + identifier + " of type " + bindType + " is already bound to another user");
        }

        // Check if user has already bound this type
        Optional<UserBinder> existing = userBinderRepository.findByUserIdAndBindType(userId, bindType);

        UserBinder userBinder;
        if (existing.isPresent()) {
            // Update existing binding
            userBinder = existing.get();
            userBinder.setIdentifier(identifier);
        } else {
            // Create new binding
            userBinder = new UserBinder();
            userBinder.setUserId(userId);
            userBinder.setBindType(bindType);
            userBinder.setIdentifier(identifier);
        }

        return userBinderRepository.save(userBinder);
    }

    /**
     * Find user ID by email
     */
    @Transactional(readOnly = true)
    public Optional<Integer> findUserIdByEmail(String email) {
        return userBinderRepository.findByIdentifierAndBindType(email, BindType.EMAIL)
                .map(UserBinder::getUserId);
    }

    /**
     * Find email by user ID
     */
    @Transactional(readOnly = true)
    public Optional<String> findEmailByUserId(Integer userId) {
        return userBinderRepository.findByUserIdAndBindType(userId, BindType.EMAIL)
                .map(UserBinder::getIdentifier);
    }

    /**
     * Check if email already exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userBinderRepository.existsByIdentifierAndBindType(email, BindType.EMAIL);
    }

    /**
     * Check if user has bound email
     */
    @Transactional(readOnly = true)
    public boolean userHasEmail(Integer userId) {
        return userBinderRepository.existsByUserIdAndBindType(userId, BindType.EMAIL);
    }

    /**
     * Get all binding information for user
     */
    @Transactional(readOnly = true)
    public List<UserBinder> getUserBinders(Integer userId) {
        return userBinderRepository.findByUserId(userId);
    }

    /**
     * Remove user's email binding
     */
    public boolean unbindEmail(Integer userId) {
        int deleted = userBinderRepository.deleteByUserIdAndBindType(userId, BindType.EMAIL);
        return deleted > 0;
    }

    /**
     * Update user's email binding
     */
    public boolean updateEmail(Integer userId, String newEmail) {
        // Check if new email is already bound by another user
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