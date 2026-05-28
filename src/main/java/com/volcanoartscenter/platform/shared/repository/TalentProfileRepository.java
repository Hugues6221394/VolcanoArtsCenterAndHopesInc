package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.TalentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TalentProfileRepository extends JpaRepository<TalentProfile, Long> {
    List<TalentProfile> findByPublishedTrueOrderByIdDesc();
}
