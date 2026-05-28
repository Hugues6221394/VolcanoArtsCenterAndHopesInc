package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bank-transfer "gateway". There is no remote API call — the buyer is shown
 * wire instructions and a unique reference, and a staff user later confirms
 * the receipt via {@code POST /api/v1/ops/payments/{paymentId}/confirm},
 * which captures the polymorphic {@link com.volcanoartscenter.platform.shared.payment.Payment}.
 */
@Service
@Slf4j
public class BankTransferPaymentService implements PaymentGatewayService {

    @Value("${platform.integrations.bank.account-name:Volcano Arts Center Inc.}")
    private String accountName;

    @Value("${platform.integrations.bank.account-number:}")
    private String accountNumber;

    @Value("${platform.integrations.bank.bank-name:}")
    private String bankName;

    @Value("${platform.integrations.bank.swift:}")
    private String swift;

    @Value("${platform.integrations.bank.iban:}")
    private String iban;

    @Override
    public String provider() { return "BANK_TRANSFER"; }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        String gatewayRef = "BNK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
        String message = String.format(
                "Wire %s %s to %s | Bank: %s | Acct: %s | SWIFT: %s | IBAN: %s | Reference: %s",
                amount.toPlainString(), currency,
                blank(accountName, "Volcano Arts Center"),
                blank(bankName, "(bank name)"),
                blank(accountNumber, "(account)"),
                blank(swift, "(swift)"),
                blank(iban, "(iban)"),
                gatewayRef);
        log.info("Bank-transfer instructions issued: ref={} amount={} {}", gatewayRef, amount, currency);
        return new PaymentResult(true, gatewayRef, message);
    }

    @Override
    public PaymentResult verify(String externalReference) {
        // Staff confirmation flips the Payment row directly; verify() is best-effort
        // and is here only so the polymorphic facade doesn't choke if it's called.
        return new PaymentResult(false, externalReference, "Awaiting staff confirmation");
    }

    public BankInstructions instructions(BigDecimal amount, String currency, String gatewayRef) {
        return new BankInstructions(
                blank(accountName, "Volcano Arts Center"),
                blank(bankName, ""),
                blank(accountNumber, ""),
                blank(swift, ""),
                blank(iban, ""),
                amount, currency, gatewayRef);
    }

    public record BankInstructions(String accountName, String bankName, String accountNumber,
                                   String swift, String iban,
                                   BigDecimal amount, String currency, String reference) {}

    private String blank(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
