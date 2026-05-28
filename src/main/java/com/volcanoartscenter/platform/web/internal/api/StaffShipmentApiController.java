package com.volcanoartscenter.platform.web.internal.api;

import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.repository.ShippingOrderRepository;
import com.volcanoartscenter.platform.shared.service.CustomsInvoiceService;
import com.volcanoartscenter.platform.shared.service.integration.impl.FedExShippingService;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Staff-side shipment ops: live FedEx tracking + commercial-invoice PDF
 * download for a ShippingOrder. Restricted to OPS_MANAGER / SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/ops/shipments")
@RequiredArgsConstructor
public class StaffShipmentApiController {

    private final ShippingOrderRepository shippingOrderRepository;
    private final FedExShippingService fedExShippingService;
    private final CustomsInvoiceService customsInvoiceService;

    @GetMapping("/{orderRef}/tracking")
    public ApiResponse<Map<String, Object>> tracking(@PathVariable String orderRef) {
        ShippingOrder order = shippingOrderRepository.findByOrderReference(orderRef)
                .orElseThrow(() -> new NotFoundException("ShippingOrder", orderRef));
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            return ApiResponse.ok(Map.of(
                    "orderRef", orderRef,
                    "trackingNumber", "",
                    "supported", false,
                    "message", "No tracking number on file for this order."
            ));
        }
        Map<String, Object> tracking = fedExShippingService.track(order.getTrackingNumber());
        Map<String, Object> body = new java.util.LinkedHashMap<>(tracking);
        body.put("orderRef", orderRef);
        body.put("carrier", order.getCarrier() == null ? null : order.getCarrier().name());
        return ApiResponse.ok(body);
    }

    @GetMapping("/{orderRef}/customs-invoice.pdf")
    public ResponseEntity<byte[]> customsInvoice(@PathVariable String orderRef) {
        ShippingOrder order = shippingOrderRepository.findByOrderReference(orderRef)
                .orElseThrow(() -> new NotFoundException("ShippingOrder", orderRef));
        byte[] pdf = customsInvoiceService.generate(order);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", orderRef + "-customs.pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
