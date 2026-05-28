package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final ProductRepository productRepository;

    public int importProducts(InputStream csvStream) throws IOException {
        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }
                String name = parts[0].trim();
                String slug = parts[1].trim();
                BigDecimal price = new BigDecimal(parts[2].trim());
                if (name.isBlank() || slug.isBlank()) {
                    continue;
                }
                Product product = Product.builder()
                        .name(name)
                        .slug(slug)
                        .price(price)
                        .description("Imported via CSV")
                        .shortDescription("Imported via CSV")
                        .inventoryType(Product.InventoryType.BATCH)
                        .stockQuantity(1)
                        .available(true)
                        .build();
                productRepository.save(product);
                imported++;
            }
        }
        return imported;
    }
}
