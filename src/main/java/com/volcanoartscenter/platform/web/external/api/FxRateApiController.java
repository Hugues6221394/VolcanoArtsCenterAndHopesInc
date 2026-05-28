package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.fx.FxRateService;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public read-only currency utilities backed by the {@code fx_rates} cache
 * + Frankfurter as upstream. Convert-on-the-fly endpoint for cart/donation
 * UIs that want to display a localized total.
 */
@RestController
@RequestMapping("/api/v1/public/fx")
@RequiredArgsConstructor
public class FxRateApiController {

    private final FxRateService fxRateService;

    @GetMapping("/rate")
    public ApiResponse<Map<String, Object>> rate(@RequestParam String from, @RequestParam String to) {
        BigDecimal rate = fxRateService.rateFor(from, to);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from.toUpperCase());
        body.put("to", to.toUpperCase());
        body.put("rate", rate);
        return ApiResponse.ok(body);
    }

    @GetMapping("/convert")
    public ApiResponse<Map<String, Object>> convert(@RequestParam BigDecimal amount,
                                                    @RequestParam String from,
                                                    @RequestParam String to) {
        BigDecimal converted = fxRateService.convert(amount, from, to);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("from", from.toUpperCase());
        body.put("to", to.toUpperCase());
        body.put("converted", converted);
        return ApiResponse.ok(body);
    }
}
