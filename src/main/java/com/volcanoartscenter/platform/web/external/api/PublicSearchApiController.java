package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.model.Experience;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.repository.ExperienceRepository;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public unified search across products + experiences using Postgres FTS.
 *
 * <p>{@code GET /api/v1/public/search?q=wood+carving&limit=10}
 *
 * <p>Accepts websearch-style queries: bare terms, quoted phrases, {@code -}
 * exclusions, and {@code OR}.
 */
@RestController
@RequestMapping("/api/v1/public/search")
@RequiredArgsConstructor
public class PublicSearchApiController {

    private static final int MAX_LIMIT = 50;

    private final ProductRepository productRepository;
    private final ExperienceRepository experienceRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        if (q == null || q.isBlank()) {
            return ApiResponse.ok(Map.of("query", "", "products", List.of(), "experiences", List.of()));
        }
        int safeLimit = Math.max(1, Math.min(MAX_LIMIT, limit));
        List<Product> products = productRepository.ftsSearch(q,
                Product.ArtworkStatus.PUBLISHED.name(), safeLimit);
        List<Experience> experiences = experienceRepository.ftsSearch(q, safeLimit);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", q);
        body.put("products", products.stream().map(this::summarizeProduct).toList());
        body.put("experiences", experiences.stream().map(this::summarizeExperience).toList());
        return ApiResponse.ok(body);
    }

    private Map<String, Object> summarizeProduct(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("slug", p.getSlug());
        m.put("name", p.getName());
        m.put("price", p.getPrice());
        m.put("imageUrl", p.getPrimaryImageUrl());
        m.put("artistName", p.getArtistName());
        return m;
    }

    private Map<String, Object> summarizeExperience(Experience e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("slug", e.getSlug());
        m.put("title", e.getTitle());
        m.put("location", e.getLocation());
        m.put("imageUrl", e.getPrimaryImageUrl());
        return m;
    }
}
