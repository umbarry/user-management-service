package com.umbarry.usermanagementservice.repository;

import com.umbarry.usermanagementservice.model.Notification;
import com.umbarry.usermanagementservice.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByUserIdAndType(Long userId, NotificationType type);
}
