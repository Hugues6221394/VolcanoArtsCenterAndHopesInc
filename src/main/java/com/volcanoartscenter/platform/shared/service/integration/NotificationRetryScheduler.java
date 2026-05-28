package com.volcanoartscenter.platform.shared.service.integration;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import com.volcanoartscenter.platform.shared.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Picks up channel deliveries that failed and whose backoff window has elapsed,
 * then re-runs them through {@link OutboundChannelService#retry(Long)}. Runs on
 * a fixed delay (default every 60s) and processes a small batch per tick to keep
 * the provider rate-limit footprint small.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationLogRepository notificationLogRepository;
    private final OutboundChannelService outboundChannelService;

    @Value("${platform.notifications.retry.batch-size:25}")
    private int batchSize;

    @Value("${platform.notifications.retry.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${platform.notifications.retry.interval-ms:60000}",
               initialDelayString = "${platform.notifications.retry.initial-delay-ms:30000}")
    public void retryFailedDeliveries() {
        if (!enabled) return;
        List<NotificationLog> due = notificationLogRepository.findRetryable(
                LocalDateTime.now(), PageRequest.of(0, Math.max(1, batchSize)));
        if (due.isEmpty()) return;
        log.info("Retrying {} failed notification deliveries.", due.size());
        for (NotificationLog row : due) {
            try {
                outboundChannelService.retry(row.getId());
            } catch (Exception ex) {
                log.warn("Retry crashed for log id={}: {}", row.getId(), ex.getMessage());
            }
        }
    }
}
