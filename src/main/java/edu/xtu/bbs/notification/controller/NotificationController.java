package edu.xtu.bbs.notification.controller;

import edu.xtu.bbs.notification.dto.NotificationResponse;
import edu.xtu.bbs.notification.service.NotificationService;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticationService authenticationService;

    public NotificationController(NotificationService notificationService,
                                  AuthenticationService authenticationService) {
        this.notificationService = notificationService;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public Page<NotificationResponse> getNotifications(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User currentUser = requireCurrentUser();
        return notificationService.getNotifications(currentUser.getId(), pageable);
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        User currentUser = requireCurrentUser();
        return notificationService.countUnread(currentUser.getId());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        User currentUser = requireCurrentUser();
        return notificationService.markAsRead(currentUser.getId(), notificationId);
    }

    @PatchMapping("/read-all")
    public int markAllAsRead() {
        User currentUser = requireCurrentUser();
        return notificationService.markAllAsRead(currentUser.getId());
    }

    private User requireCurrentUser() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }
        return currentUser;
    }
}
