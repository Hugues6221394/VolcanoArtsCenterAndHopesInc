package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.AuditEvent;
import com.volcanoartscenter.platform.shared.model.ConsentRecord;
import com.volcanoartscenter.platform.shared.repository.AuditEventRepository;
import com.volcanoartscenter.platform.shared.repository.ConsentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ConsentRecordRepository consentRecordRepository;
    private final AuditEventRepository auditEventRepository;

    public void recordConsent(String subjectEmail, String consentType, boolean consented, String source) {
        if (subjectEmail == null || subjectEmail.isBlank()) {
            return;
        }
        consentRecordRepository.save(ConsentRecord.builder()
                .subjectEmail(subjectEmail.trim().toLowerCase())
                .consentType(consentType)
                .consented(consented)
                .source(source)
                .build());
    }

    public void audit(String actorEmail, String eventType, String entityType, Long entityId, String details) {
        auditEventRepository.save(AuditEvent.builder()
                .actorEmail(actorEmail)
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
