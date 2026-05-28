package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop200ByOrderByCreatedAtDesc();
}
