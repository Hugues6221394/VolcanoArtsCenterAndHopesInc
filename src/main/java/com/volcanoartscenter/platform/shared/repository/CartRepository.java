package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUser(User user);
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByAnonSessionId(String anonSessionId);
}
