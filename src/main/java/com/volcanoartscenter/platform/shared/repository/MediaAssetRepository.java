package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    boolean existsByStorageKey(String storageKey);

    @Query("""
            SELECT m FROM MediaAsset m
            WHERE (:q IS NULL OR :q = '' OR
                   LOWER(COALESCE(m.title, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(m.altText, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(m.storageKey, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(m.publicUrl, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:contentType IS NULL OR :contentType = '' OR LOWER(COALESCE(m.contentType, '')) LIKE LOWER(CONCAT(:contentType, '%')))
            ORDER BY m.createdAt DESC
            """)
    List<MediaAsset> searchForCms(@Param("q") String q, @Param("contentType") String contentType);
}
