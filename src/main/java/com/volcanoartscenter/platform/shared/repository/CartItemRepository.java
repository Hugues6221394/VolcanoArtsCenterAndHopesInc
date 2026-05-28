package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.CartItem;
import com.volcanoartscenter.platform.shared.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
