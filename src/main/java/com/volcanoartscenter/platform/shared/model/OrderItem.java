package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ShippingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Price snapshot at the time of purchase — immutable after creation
    @Column(name = "price_at_purchase", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAtPurchase;

    // Snapshot fields for order history (in case artwork is later archived/deleted)
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_image_url")
    private String productImageUrl;

    public BigDecimal getLineTotal() {
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }
}
