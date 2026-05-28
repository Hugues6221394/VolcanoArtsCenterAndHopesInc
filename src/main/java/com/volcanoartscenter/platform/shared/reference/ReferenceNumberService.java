package com.volcanoartscenter.platform.shared.reference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Generates VAC-YYYY-NNNNN style reference numbers, atomic per (scope, year).
 *
 * <p>Counter rows live in {@code reference_counters} and are guarded with a
 * row-level pessimistic write lock during increment so two concurrent donors
 * (or applicants) never collide on the same number.
 */
@Service
@RequiredArgsConstructor
public class ReferenceNumberService {

    public static final String SCOPE_DONATION = "DONATION";
    public static final String SCOPE_TALENT = "TALENT";
    public static final String SCOPE_BOOKING = "BOOKING";

    private final ReferenceCounterRepository repo;

    /**
     * Returns the next reference for a scope, formatted as {@code VAC-YYYY-NNNNN}.
     * Always commits in its own transaction so the increment isn't held open
     * while the caller does additional work.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String scope) {
        return next(scope, LocalDate.now().getYear());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String scope, int year) {
        ReferenceCounter row = repo.findForUpdate(scope, year)
                .orElseGet(() -> repo.save(ReferenceCounter.builder()
                        .scope(scope).year(year).lastValue(0L).build()));
        long next = row.getLastValue() + 1;
        row.setLastValue(next);
        repo.save(row);
        return format(scope, year, next);
    }

    public String format(String scope, int year, long sequence) {
        return String.format(Locale.ROOT, "VAC-%d-%05d", year, sequence);
    }
}
