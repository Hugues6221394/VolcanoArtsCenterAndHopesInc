package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.ProductCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCollectionRepository extends JpaRepository<ProductCollection, Long> {
    boolean existsBySlug(String slug);
    List<ProductCollection> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
