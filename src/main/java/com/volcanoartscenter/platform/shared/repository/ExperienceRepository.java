package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Experience;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    @EntityGraph(attributePaths = {"additionalImages"})
    List<Experience> findByActiveTrueOrderByFeaturedDescTitleAsc();

    @EntityGraph(attributePaths = {"additionalImages"})
    Optional<Experience> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"additionalImages"})
    @Query("""
            SELECT e FROM Experience e
            WHERE (:active IS NULL OR e.active = :active)
              AND (:q IS NULL OR :q = '' OR
                   LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(e.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(e.location, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.featured DESC, e.title ASC
            """)
    List<Experience> searchForCms(@Param("active") Boolean active, @Param("q") String q);

    /** Postgres FTS over the {@code search_vector} column. */
    @Query(value = """
            SELECT e.* FROM experiences e
            WHERE e.active = true
              AND e.search_vector @@ websearch_to_tsquery('simple', :q)
            ORDER BY ts_rank_cd(e.search_vector, websearch_to_tsquery('simple', :q)) DESC,
                     e.featured DESC, e.title ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Experience> ftsSearch(@Param("q") String q, @Param("limit") int limit);
}
