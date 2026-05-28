package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    boolean existsBySlug(String slug);
}
