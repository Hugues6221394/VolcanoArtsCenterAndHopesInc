package com.volcanoartscenter.platform.shared.service.integration;

import java.math.BigDecimal;
import java.util.Map;

public interface ShippingCarrierService {
    String provider();
    BigDecimal estimate(String destinationCountry, BigDecimal weightKg);
    String createShipment(String reference);

    /**
     * Returns a structured tracking snapshot (carrier-defined keys). Default
     * implementation returns an empty map for carriers that don't support it
     * yet (e.g. local delivery).
     */
    default Map<String, Object> track(String trackingNumber) {
        return Map.of("trackingNumber", trackingNumber, "supported", false);
    }
}
