package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.UpdateProfileRequest;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Query Method

    @Transactional(readOnly = true)
    public User getUserById(Integer id) throws UserNotFoundException {
        if (id == null) {
            throw new UserNotFoundException("null");
        }

        final Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public UserDetails findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    /**
     * Update user's profile
     *
     * @param userId  the id of the user to be updated
     * @param profile the profile update request
     * @return true if update successful, false otherwise
     * @throws UserNotFoundException User was not exist
     */
    public boolean updateProfile(Integer userId, UpdateProfileRequest profile)
            throws UserNotFoundException {

        getUserById(userId);

        int updatedRows = userRepository.updateProfileById(
                profile.nickname(),
                profile.bio(),
                profile.profileSlug(),
                userId
        );

        return updatedRows > 0;
    }

}
