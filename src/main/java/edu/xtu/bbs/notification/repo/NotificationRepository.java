package edu.xtu.bbs.notification.repo;

import edu.xtu.bbs.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByTargetUserId(Integer targetUserId, Pageable pageable);

    Optional<Notification> findByIdAndTargetUserId(Long id, Integer targetUserId);

    long countByTargetUserIdAndReadFalse(Integer targetUserId);

    List<Notification> findByTargetUserIdAndReadFalse(Integer targetUserId);
}
