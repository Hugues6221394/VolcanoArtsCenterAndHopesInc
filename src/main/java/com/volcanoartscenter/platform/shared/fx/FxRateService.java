package com.volcanoartscenter.platform.shared.fx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Currency conversion against the cached {@code fx_rates} table. On a cache miss
 * (or a row older than the configured TTL), one row-level pessimistic write lock
 * is taken — concurrent callers wait for the same upstream fetch instead of
 * stampeding the Frankfurter API.
 *
 * <p>Frankfurter is a free, no-key ECB-backed FX API:
 * {@code https://api.frankfurter.app/latest?from=USD&to=EUR}.
 */
@Service
@Slf4j
public class FxRateService {

    private final FxRateRepository repo;
    private final RestClient restClient = RestClient.builder().build();
    private final String apiBase;
    private final Duration ttl;

    @Autowired
    @Lazy
    private FxRateService self;

    public FxRateService(FxRateRepository repo,
                         @Value("${platform.integrations.fx.frankfurter-base:https://api.frankfurter.app}") String apiBase,
                         @Value("${platform.integrations.fx.cache-ttl-hours:24}") long cacheTtlHours) {
        this.repo = repo;
        this.apiBase = trimTrailing(apiBase);
        this.ttl = Duration.ofHours(Math.max(1, cacheTtlHours));
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (amount == null) return null;
        String base = normalize(from);
        String quote = normalize(to);
        if (base.equals(quote)) return amount;
        BigDecimal rate = rateFor(base, quote);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal rateFor(String from, String to) {
        String base = normalize(from);
        String quote = normalize(to);
        if (base.equals(quote)) return BigDecimal.ONE;
        FxRate cached = repo.findById(new FxRate.Pk(base, quote)).orElse(null);
        if (cached != null && fresh(cached)) return cached.getRate();
        // Self-invocation through the @Lazy proxy so the write transaction's
        // PESSIMISTIC_WRITE lock is honoured.
        return self.refresh(base, quote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BigDecimal refresh(String base, String quote) {
        FxRate row = repo.findForUpdate(base, quote).orElse(null);
        if (row != null && fresh(row)) {
            return row.getRate();
        }
        BigDecimal upstream = fetchFromFrankfurter(base, quote);
        if (row == null) {
            row = FxRate.builder()
                    .baseCurrency(base)
                    .quoteCurrency(quote)
                    .rate(upstream)
                    .fetchedAt(LocalDateTime.now())
                    .source("frankfurter")
                    .build();
        } else {
            row.setRate(upstream);
            row.setFetchedAt(LocalDateTime.now());
        }
        repo.save(row);
        log.info("FX rate refreshed: 1 {} = {} {}", base, upstream, quote);
        return upstream;
    }

    private FxRateService self() {
        return org.springframework.aop.framework.AopContext.currentProxy() instanceof FxRateService proxy
                ? proxy : this;
    }

    private BigDecimal fetchFromFrankfurter(String base, String quote) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(apiBase + "/latest?from={base}&to={quote}", base, quote)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("rates") instanceof Map<?, ?> rates)
                    || !rates.containsKey(quote)) {
                throw new IllegalStateException("Frankfurter response missing rate for " + base + "->" + quote);
            }
            return new BigDecimal(String.valueOf(rates.get(quote)));
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Frankfurter " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    private boolean fresh(FxRate r) {
        return r.getFetchedAt() != null
                && r.getFetchedAt().isAfter(LocalDateTime.now().minus(ttl));
    }

    private String normalize(String c) {
        if (c == null || c.isBlank()) {
            throw new IllegalArgumentException("Currency code is required");
        }
        return c.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimTrailing(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
    }
}
