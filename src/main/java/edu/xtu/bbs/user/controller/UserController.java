package edu.xtu.bbs.user.controller;

import edu.xtu.bbs.user.dto.UpdateProfileRequest;
import edu.xtu.bbs.user.exception.IllegalContentTypeException;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.service.AvatarService;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.user.vo.AvatarUploadVo;
import edu.xtu.bbs.user.vo.UploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final AvatarService avatarService;

    public UserController(AuthenticationService authenticationService, UserService userService, AvatarService avatarService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.avatarService = avatarService;
    }

    @GetMapping
    public UserDetails getCurrentUser() {
        return authenticationService.getCurrentUser();
    }

    @GetMapping("/{username}")
    public UserDetails getUserByUsername(@PathVariable("username") String username) {
        return userService.findByUsername(username);
    }

    @PostMapping("/{username}/avatar")
    public AvatarUploadVo getAvatarUploadUrl(@PathVariable("username") String username, @RequestBody UploadRequest request) throws FileSizeLimitExceededException, IllegalContentTypeException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid upload request");
        }

        final UserDetails currentUser = authenticationService.getCurrentUser();
        if (!currentUser.getUsername().equals(username)) {
            throw new SecurityException("You can only upload avatar for your own account.");
        }

        final String uploadUrl = avatarService.generateAvatarUploadUrl(currentUser.getUsername(), request.type(), DataSize.ofBytes(request.size()));
        final String accessUrl = avatarService.generateAvatarUrl(currentUser.getUsername());
        return new AvatarUploadVo(uploadUrl, accessUrl);
    }

    @PatchMapping("/{username}/profile")
    public UserDetails updateProfile(@PathVariable("username") String username, @RequestBody UpdateProfileRequest request) throws UserNotFoundException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid update profile request");
        }

        final User currentUser = authenticationService.getCurrentUser();
        if (!currentUser.getUsername().equals(username)) {
            throw new SecurityException("You can only update your own profile.");
        }

        if (!userService.updateProfile(currentUser.getId(), request)) {
            throw new RuntimeException("Failed to update profile.");
        }

        return userService.findByUsername(username);
    }


}
