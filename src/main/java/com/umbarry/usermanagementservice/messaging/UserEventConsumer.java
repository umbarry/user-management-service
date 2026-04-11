package com.umbarry.usermanagementservice.messaging;

import com.umbarry.usermanagementservice.config.RabbitMQConfig;
import com.umbarry.usermanagementservice.events.UserCreatedEvent;
import com.umbarry.usermanagementservice.model.Notification;
import com.umbarry.usermanagementservice.model.NotificationType;
import com.umbarry.usermanagementservice.model.User;
import com.umbarry.usermanagementservice.repository.NotificationRepository;
import com.umbarry.usermanagementservice.repository.UserRepository;
import com.umbarry.usermanagementservice.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventConsumer {

    private final MailService mailService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitMQConfig.USER_CREATED_QUEUE)
    @Transactional
    public void consumeUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user created event for user ID: {}", event.getUserId());

        // Idempotency check
        if (notificationRepository.findByUserIdAndType(event.getUserId(), NotificationType.WELCOME_EMAIL).isPresent()) {
            log.info("Welcome email already sent for user ID: {}. Skipping.", event.getUserId());
            return;
        }

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + event.getUserId()));

        try {
            mailService.sendWelcomeEmail(event.getEmail(), event.getUsername());
            
            Notification notification = Notification.builder()
                    .user(user)
                    .type(NotificationType.WELCOME_EMAIL)
                    .sent(true)
                    .build();
            notificationRepository.save(notification);
            
            log.info("Welcome email sent and recorded for user ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to send welcome email for user ID: {}", event.getUserId(), e);
            // In a real scenario, we might want to retry or send to a DLQ
        }
    }
}
