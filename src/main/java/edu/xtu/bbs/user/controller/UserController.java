package edu.xtu.bbs.user.controller;

import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public UserController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @GetMapping
    public UserDetails getCurrentUser() {
        return authenticationService.getCurrentUser();
    }

    @GetMapping("/{username}")
    public UserDetails getUserByUsername(@PathVariable("username") String username) {
        return userService.findByUsername(username);
    }
}
