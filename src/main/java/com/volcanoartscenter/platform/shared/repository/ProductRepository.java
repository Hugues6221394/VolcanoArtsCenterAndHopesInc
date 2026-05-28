package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByReservedUntilBefore(LocalDateTime reservedUntil);

    @EntityGraph(attributePaths = "category")
    List<Product> findByAvailableTrueOrderByFeaturedDescNameAsc();

    @EntityGraph(attributePaths = "category")
    List<Product> findTop8ByAvailableTrueAndArtworkStatusOrderByFeaturedDescCreatedAtDesc(Product.ArtworkStatus artworkStatus);

    @EntityGraph(attributePaths = "category")
    List<Product> findByAvailableTrueAndFeaturedTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByAvailableTrueAndCategory_SlugOrderByFeaturedDescNameAsc(String slug);

    @EntityGraph(attributePaths = {"category", "collection", "additionalImages"})
    Optional<Product> findBySlugAndAvailableTrueAndArtworkStatus(String slug, Product.ArtworkStatus artworkStatus);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByAvailableTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByAvailableTrueAndCategory_Slug(String slug, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
            WHERE p.available = true
              AND p.artworkStatus = :artworkStatus
              AND (:category IS NULL OR p.category.slug = :category)
              AND (:q IS NULL OR :q = '' OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                   LOWER(COALESCE(p.artistName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> searchCatalog(@Param("category") String category,
                                @Param("q") String q,
                                @Param("minPrice") java.math.BigDecimal minPrice,
                                @Param("maxPrice") java.math.BigDecimal maxPrice,
                                @Param("artworkStatus") Product.ArtworkStatus artworkStatus,
                                Pageable pageable);

    boolean existsBySlug(String slug);

    /**
     * Postgres FTS over the {@code search_vector} column. {@code q} should be
     * a websearch-style query string (e.g. {@code "wood carving"} or
     * {@code "carving -ceramic"}). Returns rank-ordered results.
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.available = true
              AND p.artwork_status = :status
              AND p.search_vector @@ websearch_to_tsquery('simple', :q)
            ORDER BY ts_rank_cd(p.search_vector, websearch_to_tsquery('simple', :q)) DESC,
                     p.featured DESC, p.name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> ftsSearch(@Param("q") String q,
                            @Param("status") String status,
                            @Param("limit") int limit);

    @EntityGraph(attributePaths = {"category", "collection", "additionalImages"})
    @Query("""
            SELECT p FROM Product p
            WHERE (:available IS NULL OR p.available = :available)
              AND (:featured IS NULL OR p.featured = :featured)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:collectionId IS NULL OR p.collection.id = :collectionId)
              AND (:q IS NULL OR :q = '' OR
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.shortDescription, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.artistName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(COALESCE(p.slug, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.featured DESC, p.name ASC
            """)
    List<Product> searchForCms(@Param("available") Boolean available,
                               @Param("featured") Boolean featured,
                               @Param("categoryId") Long categoryId,
                               @Param("collectionId") Long collectionId,
                               @Param("q") String q);
}
