package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {
    boolean existsByExternalEventId(String externalEventId);
}
