package com.volcanoartscenter.platform.web.external.touroperator.controller;

import com.volcanoartscenter.platform.shared.model.TourOperatorRequest;
import com.volcanoartscenter.platform.web.external.touroperator.service.TourOperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class TourOperatorController {

    private final TourOperatorService tourOperatorService;

    @GetMapping("/tour-operators/portal")
    public String operatorPortal(Authentication authentication, Model model) {
        String operatorEmail = currentEmail(authentication);
        if (operatorEmail == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentPage", "contact");
        model.addAttribute("pageTitle", "Tour Operator Portal");
        model.addAttribute("email", operatorEmail);
        model.addAttribute("requests", tourOperatorService.listOwnerRequests(operatorEmail));
        model.addAttribute("stats", tourOperatorService.buildStats(operatorEmail));
        model.addAttribute("experiences", tourOperatorService.activeExperiences());
        model.addAttribute("catalogProducts", tourOperatorService.availableProducts());
        model.addAttribute("latestConfirmed", tourOperatorService.latestConfirmed(operatorEmail).orElse(null));
        return "external/tour-operator/portal";
    }

    @GetMapping("/tour-operators/register")
    public String registerPage(Model model) {
        model.addAttribute("currentPage", "contact");
        model.addAttribute("pageTitle", "Tour Operator Registration");
        return "external/tour-operator/register";
    }

    @PostMapping("/tour-operators/register")
    public String register(@RequestParam String companyName,
                           @RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String country,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {
        try {
            tourOperatorService.registerOperatorAccount(companyName, firstName, lastName, email, phone, country, password);
            redirectAttributes.addFlashAttribute("successMessage", "Tour operator account created. Please sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
            return "redirect:/tour-operators/register";
        }
    }

    @PostMapping("/tour-operators/portal/requests")
    public String submitRequest(Authentication authentication,
                                @RequestParam String companyName,
                                @RequestParam String contactName,
                                @RequestParam(required = false) String contactPhone,
                                @RequestParam(required = false) String country,
                                @RequestParam TourOperatorRequest.RequestType requestType,
                                @RequestParam(required = false) String requestedExperienceSlug,
                                @RequestParam(defaultValue = "1") Integer estimatedGroupSize,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate estimatedDate,
                                @RequestParam(defaultValue = "true") Boolean invoiceRequired,
                                @RequestParam(required = false) String requestDetails,
                                @RequestParam(defaultValue = "EMAIL") String preferredContactChannel,
                                RedirectAttributes redirectAttributes) {
        String operatorEmail = currentEmail(authentication);
        if (operatorEmail == null) {
            return "redirect:/login";
        }
        try {
            TourOperatorRequest request = tourOperatorService.createRequest(
                    operatorEmail, companyName, contactName, contactPhone, country, requestType, requestedExperienceSlug,
                    estimatedGroupSize, estimatedDate, invoiceRequired, requestDetails, preferredContactChannel
            );
            redirectAttributes.addFlashAttribute("successMessage", "Request submitted successfully. Reference #" + request.getId() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/tour-operators/portal";
    }

    @PostMapping("/tour-operators/portal/requests/{id}/update")
    public String updateRequest(Authentication authentication,
                                @PathVariable Long id,
                                @RequestParam(defaultValue = "1") Integer estimatedGroupSize,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate estimatedDate,
                                @RequestParam(required = false) String requestDetails,
                                @RequestParam(defaultValue = "EMAIL") String preferredContactChannel,
                                RedirectAttributes redirectAttributes) {
        String operatorEmail = currentEmail(authentication);
        if (operatorEmail == null) {
            return "redirect:/login";
        }
        try {
            tourOperatorService.updateRequestDetails(operatorEmail, id, estimatedGroupSize, estimatedDate, requestDetails, preferredContactChannel);
            redirectAttributes.addFlashAttribute("successMessage", "Request updated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/tour-operators/portal";
    }

    @PostMapping("/tour-operators/portal/requests/{id}/cancel")
    public String cancelRequest(Authentication authentication, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        String operatorEmail = currentEmail(authentication);
        if (operatorEmail == null) {
            return "redirect:/login";
        }
        try {
            tourOperatorService.cancelRequest(operatorEmail, id);
            redirectAttributes.addFlashAttribute("successMessage", "Request cancelled.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/tour-operators/portal";
    }

    private String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name.trim().toLowerCase();
    }
}
