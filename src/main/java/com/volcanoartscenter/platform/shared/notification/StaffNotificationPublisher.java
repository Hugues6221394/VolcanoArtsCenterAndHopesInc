package com.volcanoartscenter.platform.shared.notification;

import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fan-out in-app notifications to all platform staff accounts.
 */
@Service
@RequiredArgsConstructor
public class StaffNotificationPublisher {

    private static final List<String> STAFF_ROLES =
            List.of("SUPER_ADMIN", "CONTENT_MANAGER", "OPS_MANAGER");

    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    public void notifyAllStaff(NotificationCategory category, String title, String body,
                               String ctaUrl, String entityType, Long entityId) {
        List<User> staff = userRepository.findByRoles_NameIn(STAFF_ROLES);
        for (User member : staff) {
            events.publishEvent(NotificationEvent.forEntity(
                    member.getId(), category, title, body, ctaUrl, entityType, entityId));
        }
    }
}
