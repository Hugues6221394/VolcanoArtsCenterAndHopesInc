package com.volcanoartscenter.platform.shared.service.integration;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import com.volcanoartscenter.platform.shared.repository.NotificationLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persist-before-send orchestrator. Writes a {@link NotificationLog} in PENDING,
 * invokes the right channel adapter, and updates the row to SENT / FAILED with
 * attempt-count + exponential backoff for the retry scheduler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundChannelService {

    private final NotificationLogRepository notificationLogRepository;
    private final List<MessagingService> messagingServices;

    @Value("${platform.notifications.retry.max-attempts:5}")
    private int defaultMaxAttempts;

    @Value("${platform.notifications.retry.base-delay-seconds:60}")
    private long baseDelaySeconds;

    @Value("${platform.notifications.retry.max-delay-seconds:3600}")
    private long maxDelaySeconds;

    private Map<NotificationLog.Channel, MessagingService> adapterIndex;

    @PostConstruct
    void initIndex() {
        adapterIndex = new HashMap<>();
        for (MessagingService svc : messagingServices) {
            try {
                adapterIndex.put(NotificationLog.Channel.valueOf(svc.channel().toUpperCase()), svc);
            } catch (IllegalArgumentException ex) {
                log.warn("Messaging adapter {} reports unknown channel '{}' — skipped",
                        svc.getClass().getSimpleName(), svc.channel());
            }
        }
    }

    /**
     * Persists a PENDING log, invokes the adapter, and updates the row in a
     * separate transaction so callers' business writes remain isolated.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationLog deliver(NotificationLog.Channel channel,
                                   String recipient,
                                   String subject,
                                   String body) {
        NotificationLog log = notificationLogRepository.save(NotificationLog.builder()
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .messageBody(body)
                .status(NotificationLog.DeliveryStatus.PENDING)
                .attempts(0)
                .maxAttempts(defaultMaxAttempts)
                .build());
        return attempt(log);
    }

    /** Retry an existing log row. Used by RetryScheduler. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationLog retry(Long logId) {
        NotificationLog log = notificationLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("NotificationLog " + logId + " not found"));
        if (log.getStatus() == NotificationLog.DeliveryStatus.SENT
                || log.getAttempts() >= log.getMaxAttempts()) {
            return log;
        }
        return attempt(log);
    }

    private NotificationLog attempt(NotificationLog logRow) {
        MessagingService adapter = adapterIndex.get(logRow.getChannel());
        LocalDateTime now = LocalDateTime.now();
        logRow.setAttempts(logRow.getAttempts() + 1);
        logRow.setLastAttemptAt(now);

        if (adapter == null) {
            return finishFailure(logRow, "No adapter configured for channel " + logRow.getChannel(), now);
        }
        if (!adapter.isConfigured()) {
            return finishFailure(logRow,
                    logRow.getChannel() + " adapter not configured (missing credentials)", now);
        }
        try {
            String externalRef = adapter.send(logRow.getRecipient(), logRow.getSubject(), logRow.getMessageBody());
            logRow.setStatus(NotificationLog.DeliveryStatus.SENT);
            logRow.setExternalReference(externalRef);
            logRow.setLastError(null);
            logRow.setNextAttemptAt(null);
            log.info("Delivered {} to {} (logId={}, ref={})",
                    logRow.getChannel(), logRow.getRecipient(), logRow.getId(), externalRef);
            return notificationLogRepository.save(logRow);
        } catch (Exception ex) {
            return finishFailure(logRow, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), now);
        }
    }

    private NotificationLog finishFailure(NotificationLog logRow, String reason, LocalDateTime now) {
        logRow.setStatus(NotificationLog.DeliveryStatus.FAILED);
        logRow.setLastError(truncate(reason, 4000));
        if (logRow.getAttempts() < logRow.getMaxAttempts()) {
            logRow.setNextAttemptAt(now.plusSeconds(backoffSeconds(logRow.getAttempts())));
            log.warn("Delivery failed (will retry): channel={} recipient={} attempt={}/{} reason={}",
                    logRow.getChannel(), logRow.getRecipient(), logRow.getAttempts(),
                    logRow.getMaxAttempts(), reason);
        } else {
            logRow.setNextAttemptAt(null);
            log.error("Delivery exhausted: channel={} recipient={} attempts={} reason={}",
                    logRow.getChannel(), logRow.getRecipient(), logRow.getAttempts(), reason);
        }
        return notificationLogRepository.save(logRow);
    }

    private long backoffSeconds(int attempt) {
        long delay = baseDelaySeconds * (1L << Math.min(attempt - 1, 16));
        return Math.min(delay, maxDelaySeconds);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
