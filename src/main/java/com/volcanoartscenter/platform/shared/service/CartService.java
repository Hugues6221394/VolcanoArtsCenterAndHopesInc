package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.exception.BusinessRuleException;
import com.volcanoartscenter.platform.shared.exception.ConflictException;
import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.CartItem;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.CartItemRepository;
import com.volcanoartscenter.platform.shared.repository.CartRepository;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import com.volcanoartscenter.platform.shared.reservation.ProductReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductReservationService reservationService;

    public Cart getOrCreateCart(User user, String anonSessionId) {
        if (user != null) {
            return cartRepository.findByUser(user).orElseGet(() ->
                    cartRepository.save(Cart.builder().user(user).build()));
        }
        if (anonSessionId != null) {
            return cartRepository.findByAnonSessionId(anonSessionId).orElseGet(() ->
                    cartRepository.save(Cart.builder().anonSessionId(anonSessionId).build()));
        }
        throw new IllegalArgumentException("Must provide either user or anonymous session ID");
    }

    @Transactional
    public void addItemToCart(User user, String anonSessionId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        if (!product.isInStock()) {
            throw new BusinessRuleException("OUT_OF_STOCK", "This piece is no longer available.");
        }

        String holder = holderKey(user, anonSessionId);

        // Atomic reservation for unique artworks (PRD §4.1).
        if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
            boolean reserved = reservationService.tryReserve(productId, holder);
            if (!reserved) {
                long retryAfter = reservationService.remainingSeconds(productId).orElse(0L);
                throw new ConflictException(
                        "ARTWORK_RESERVED",
                        "This piece is currently reserved. It will become available in " + Math.max(1, (retryAfter + 59) / 60) + " minute(s).",
                        Map.of("productId", productId, "retryAfterSeconds", retryAfter));
            }
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + quantity;
            if (product.getInventoryType() == Product.InventoryType.BATCH
                    && product.getStockQuantity() != null
                    && newQty > product.getStockQuantity()) {
                throw new BusinessRuleException("INSUFFICIENT_STOCK",
                        "Only " + product.getStockQuantity() + " in stock.");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build());
        }
    }

    @Transactional
    public void removeItemFromCart(User user, String anonSessionId, Long productId) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            cartItemRepository.delete(item);
            if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
                reservationService.release(productId, holderKey(user, anonSessionId));
            }
        });
    }

    @Transactional
    public void clearCart(User user, String anonSessionId) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        String holder = holderKey(user, anonSessionId);
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
                reservationService.release(product.getId(), holder);
            }
        }
        cartItemRepository.deleteAll(cart.getItems());
    }

    private static String holderKey(User user, String anonSessionId) {
        if (user != null) return "u:" + user.getId();
        return "s:" + anonSessionId;
    }
}
