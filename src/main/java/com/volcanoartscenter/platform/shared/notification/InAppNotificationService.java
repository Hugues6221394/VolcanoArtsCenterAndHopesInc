package com.volcanoartscenter.platform.shared.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InAppNotificationService {

    private final NotificationRepository repo;

    public InAppNotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public long countUnread(Long userId) {
        if (userId == null) return 0L;
        return repo.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    public List<Notification> recent(Long userId) {
        if (userId == null) return List.of();
        return repo.findTop10ByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Notification> inbox(Long userId, int page, int size, NotificationCategory category, boolean unreadOnly) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(5, size)));
        if (category != null) {
            return repo.findByRecipientUserIdAndCategoryOrderByCreatedAtDesc(userId, category, pr);
        }
        if (unreadOnly) {
            return repo.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pr);
        }
        return repo.findByRecipientUserIdOrderByCreatedAtDesc(userId, pr);
    }

    @Transactional
    public boolean markRead(Long userId, Long notificationId) {
        return repo.markRead(notificationId, userId, LocalDateTime.now()) > 0;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repo.markAllRead(userId, LocalDateTime.now());
    }
}
