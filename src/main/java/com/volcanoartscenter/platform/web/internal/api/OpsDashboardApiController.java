package com.volcanoartscenter.platform.web.internal.api;

import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.payment.Payment;
import com.volcanoartscenter.platform.shared.payment.PaymentRepository;
import com.volcanoartscenter.platform.shared.payment.PaymentStatus;
import com.volcanoartscenter.platform.shared.repository.BookingRepository;
import com.volcanoartscenter.platform.shared.repository.ContactInquiryRepository;
import com.volcanoartscenter.platform.shared.repository.DonationRepository;
import com.volcanoartscenter.platform.shared.repository.ShippingOrderRepository;
import com.volcanoartscenter.platform.shared.repository.TalentApplicationRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated operations KPIs for the staff dashboard. Read-only.
 *
 * <p>Computes counts in-memory off small result sets — fine for a single-tenant
 * arts center but trivially swappable to native COUNT/SUM aggregates if the
 * dataset grows.
 *
 * <p>Restricted to OPS_MANAGER / SUPER_ADMIN by SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/ops/dashboard")
@RequiredArgsConstructor
public class OpsDashboardApiController {

    private final ShippingOrderRepository shippingOrderRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final DonationRepository donationRepository;
    private final TalentApplicationRepository talentApplicationRepository;
    private final ContactInquiryRepository contactInquiryRepository;

    @GetMapping("/kpis")
    public ApiResponse<Map<String, Object>> kpis() {
        List<ShippingOrder> orders = shippingOrderRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        long donationsCount = donationRepository.count();
        long talentCount = talentApplicationRepository.count();
        long inquiriesCount = contactInquiryRepository.count();

        BigDecimal revenueCaptured = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CAPTURED && p.getAmount() != null)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueLast30d = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.CAPTURED
                        && p.getCapturedAt() != null
                        && p.getCapturedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        EnumMap<ShippingOrder.OrderStatus, Long> ordersByStatus = new EnumMap<>(ShippingOrder.OrderStatus.class);
        for (ShippingOrder.OrderStatus s : ShippingOrder.OrderStatus.values()) ordersByStatus.put(s, 0L);
        orders.forEach(o -> ordersByStatus.merge(o.getStatus(), 1L, Long::sum));

        EnumMap<Booking.BookingStatus, Long> bookingsByStatus = new EnumMap<>(Booking.BookingStatus.class);
        for (Booking.BookingStatus s : Booking.BookingStatus.values()) bookingsByStatus.put(s, 0L);
        bookings.forEach(b -> bookingsByStatus.merge(b.getStatus(), 1L, Long::sum));

        Map<String, Long> paymentsByGateway = new java.util.LinkedHashMap<>();
        payments.forEach(p -> paymentsByGateway.merge(p.getGateway().name(), 1L, Long::sum));

        Map<String, Long> paymentsByStatus = new java.util.LinkedHashMap<>();
        payments.forEach(p -> paymentsByStatus.merge(p.getStatus().name(), 1L, Long::sum));

        long ordersToday = orders.stream()
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        long bookingsUpcoming = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                        && b.getPreferredDate() != null
                        && !b.getPreferredDate().isBefore(LocalDate.now()))
                .count();

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("revenueCaptured", revenueCaptured);
        body.put("revenueLast30d", revenueLast30d);
        body.put("ordersTotal", orders.size());
        body.put("ordersToday", ordersToday);
        body.put("ordersByStatus", ordersByStatus);
        body.put("bookingsTotal", bookings.size());
        body.put("bookingsUpcoming", bookingsUpcoming);
        body.put("bookingsByStatus", bookingsByStatus);
        body.put("paymentsByGateway", paymentsByGateway);
        body.put("paymentsByStatus", paymentsByStatus);
        body.put("donationsTotal", donationsCount);
        body.put("talentApplicationsTotal", talentCount);
        body.put("contactInquiriesTotal", inquiriesCount);
        body.put("generatedAt", LocalDateTime.now().toString());
        return ApiResponse.ok(body);
    }
}
