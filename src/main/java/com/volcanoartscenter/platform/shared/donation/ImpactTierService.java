package com.volcanoartscenter.platform.shared.donation;

import com.volcanoartscenter.platform.shared.model.DonationCampaign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses {@link DonationCampaign#getImpactTiers()} (delimited string) and
 * picks the highest tier whose threshold the donation amount has reached.
 *
 * <p>Format examples (semicolon-separated tiers, pipe-separated amount/label):
 * <pre>
 *   10|Plant a tree;50|Sponsor a child for a week;100|Fund a workshop
 *   USD 25 | A reading session;USD 100 | A guided cultural tour
 * </pre>
 *
 * <p>Whitespace and a leading currency word are tolerated. Returns {@code null}
 * if the campaign has no tiers configured or the donation hasn't reached any.
 */
@Service
@Slf4j
public class ImpactTierService {

    public record Tier(BigDecimal threshold, String label) {}

    public String labelFor(DonationCampaign campaign, BigDecimal amount) {
        Tier reached = highestReachedTier(campaign, amount);
        return reached == null ? null : reached.label();
    }

    public Tier highestReachedTier(DonationCampaign campaign, BigDecimal amount) {
        if (campaign == null || amount == null) return null;
        List<Tier> tiers = parse(campaign.getImpactTiers());
        if (tiers.isEmpty()) return null;
        return tiers.stream()
                .filter(t -> amount.compareTo(t.threshold()) >= 0)
                .max(Comparator.comparing(Tier::threshold))
                .orElse(null);
    }

    public List<Tier> parse(String impactTiers) {
        List<Tier> out = new ArrayList<>();
        if (impactTiers == null || impactTiers.isBlank()) return out;
        for (String raw : impactTiers.split(";")) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            int pipe = entry.indexOf('|');
            if (pipe < 0) continue;
            String left = entry.substring(0, pipe).trim();
            String label = entry.substring(pipe + 1).trim();
            BigDecimal threshold = parseAmount(left);
            if (threshold == null || label.isEmpty()) continue;
            out.add(new Tier(threshold, label));
        }
        return out;
    }

    private BigDecimal parseAmount(String s) {
        String numeric = s.replaceAll("[^0-9.\\-]", "");
        if (numeric.isEmpty()) return null;
        try {
            return new BigDecimal(numeric);
        } catch (NumberFormatException ex) {
            log.warn("Skipping unparseable impact-tier amount: '{}'", s);
            return null;
        }
    }
}
