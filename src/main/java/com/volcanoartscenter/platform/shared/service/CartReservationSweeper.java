package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled sweeper that releases unique artworks whose 15-minute 
 * reservation lock has expired. Replaces the need for a Redis TTL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartReservationSweeper {

    private final ProductRepository productRepository;

    @Scheduled(fixedRate = 60_000) // check every minute
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> expiredReservations = productRepository.findByReservedUntilBefore(now);
        
        for (Product product : expiredReservations) {
            product.setReservedQuantity(0);
            product.setReservedUntil(null);
            productRepository.save(product);
            log.info("Released expired reservation for artwork: '{}' (id={})", product.getName(), product.getId());
        }

        if (!expiredReservations.isEmpty()) {
            log.info("CartReservationSweeper: Released {} expired artwork reservations.", expiredReservations.size());
        }
    }
}
