package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("""
            SELECT n FROM NotificationLog n
             WHERE n.status <> com.volcanoartscenter.platform.shared.model.NotificationLog.DeliveryStatus.SENT
               AND n.attempts < n.maxAttempts
               AND (n.nextAttemptAt IS NULL OR n.nextAttemptAt <= :now)
             ORDER BY n.nextAttemptAt ASC NULLS FIRST
            """)
    List<NotificationLog> findRetryable(LocalDateTime now, Pageable pageable);
}
