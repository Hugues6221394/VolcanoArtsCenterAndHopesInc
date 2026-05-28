package com.volcanoartscenter.platform.shared.notification;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.service.integration.OutboundChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Single entry point for emitting user-facing notifications. Persists the in-app
 * row, then fans the same event out to email and (for high-priority categories)
 * SMS via {@link OutboundChannelService}, which handles persist-before-send and
 * retry bookkeeping. WhatsApp is currently opt-in only and is not auto-fanned.
 *
 * <p>Always runs in its own transaction so a failure in any channel never rolls
 * back the originating business write.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    /** Categories that warrant an SMS in addition to email. */
    private static final Set<NotificationCategory> SMS_CATEGORIES = EnumSet.of(
            NotificationCategory.PAYMENT_FAILED,
            NotificationCategory.ORDER_SHIPPED,
            NotificationCategory.ORDER_DELIVERED,
            NotificationCategory.BOOKING_CONFIRMED,
            NotificationCategory.BOOKING_REJECTED,
            NotificationCategory.PASSWORD_RESET);

    /** Categories that should NOT fan out to email (staff-only inbox events). */
    private static final Set<NotificationCategory> IN_APP_ONLY = EnumSet.of(
            NotificationCategory.STAFF_NEW_ORDER,
            NotificationCategory.STAFF_NEW_BOOKING,
            NotificationCategory.STAFF_NEW_TALENT_APPLICATION,
            NotificationCategory.STAFF_NEW_INQUIRY,
            NotificationCategory.STAFF_NEW_OPERATOR_REQUEST);

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final OutboundChannelService outboundChannelService;

    public NotificationDispatcher(NotificationRepository repo,
                                  UserRepository userRepository,
                                  OutboundChannelService outboundChannelService) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.outboundChannelService = outboundChannelService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification dispatch(NotificationEvent event) {
        if (event.recipientUserId() == null) {
            log.warn("NotificationEvent dropped — null recipient (category={})", event.category());
            return null;
        }
        Notification notification = Notification.builder()
                .recipientUserId(event.recipientUserId())
                .category(event.category())
                .title(event.title())
                .body(event.body())
                .ctaUrl(event.ctaUrl())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .build();
        Notification saved = repo.save(notification);
        log.debug("Dispatched in-app notification id={} userId={} category={}",
                saved.getId(), saved.getRecipientUserId(), saved.getCategory());

        if (IN_APP_ONLY.contains(event.category())) {
            return saved;
        }

        userRepository.findById(event.recipientUserId()).ifPresent(user -> fanOut(user, event));
        return saved;
    }

    private void fanOut(User user, NotificationEvent event) {
        String email = user.getEmail();
        if (email != null && !email.isBlank()) {
            try {
                outboundChannelService.deliver(NotificationLog.Channel.EMAIL,
                        email, event.title(), composeEmailBody(event));
            } catch (Exception ex) {
                log.warn("Email fan-out crashed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        if (SMS_CATEGORIES.contains(event.category())) {
            String phone = user.getPhone();
            if (phone != null && !phone.isBlank()) {
                try {
                    outboundChannelService.deliver(NotificationLog.Channel.SMS,
                            phone, event.title(), event.body());
                } catch (Exception ex) {
                    log.warn("SMS fan-out crashed for user {}: {}", user.getId(), ex.getMessage());
                }
            }
        }
    }

    private String composeEmailBody(NotificationEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>").append(escape(event.body() == null ? "" : event.body())).append("</p>");
        if (event.ctaUrl() != null && !event.ctaUrl().isBlank()) {
            sb.append("<p><a href=\"").append(event.ctaUrl()).append("\">View details</a></p>");
        }
        sb.append("<p style=\"color:#888;font-size:12px\">— Volcano Arts Center</p>");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
