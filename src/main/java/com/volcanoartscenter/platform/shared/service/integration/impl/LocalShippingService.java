package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.ShippingCarrierService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LocalShippingService implements ShippingCarrierService {
    @Override
    public String provider() {
        return "LOCAL";
    }

    @Override
    public BigDecimal estimate(String destinationCountry, BigDecimal weightKg) {
        return new BigDecimal("10.00");
    }

    @Override
    public String createShipment(String reference) {
        return "LOCAL-" + reference;
    }
}
