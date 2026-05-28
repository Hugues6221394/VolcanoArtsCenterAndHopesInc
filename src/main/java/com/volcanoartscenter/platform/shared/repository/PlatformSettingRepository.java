package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    List<PlatformSetting> findByCategoryOrderByKeyNameAsc(String category);
    Optional<PlatformSetting> findByCategoryAndKeyName(String category, String keyName);
}
