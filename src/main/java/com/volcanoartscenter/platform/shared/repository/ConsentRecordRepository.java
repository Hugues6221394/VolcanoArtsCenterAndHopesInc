package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {
}
