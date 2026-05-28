package com.volcanoartscenter.platform.shared.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    List<Notification> findTop10ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndCategoryOrderByCreatedAtDesc(Long recipientUserId,
                                                                            NotificationCategory category,
                                                                            Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.id = :id AND n.recipientUserId = :userId AND n.readAt IS NULL")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientUserId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
