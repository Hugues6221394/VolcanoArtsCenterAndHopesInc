package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.SavedItem;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedItemRepository extends JpaRepository<SavedItem, Long> {
    List<SavedItem> findByUserOrderByCreatedAtDesc(User user);
    Optional<SavedItem> findByUserAndProduct(User user, Product product);
    void deleteByUserAndProduct(User user, Product product);
}
