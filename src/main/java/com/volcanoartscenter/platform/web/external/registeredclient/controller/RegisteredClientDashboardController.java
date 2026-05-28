package com.volcanoartscenter.platform.web.external.registeredclient.controller;

import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.web.external.registeredclient.service.RegisteredClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegisteredClientDashboardController {

    private final RegisteredClientService registeredClientService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("currentPage", "home");
        model.addAttribute("pageTitle", "Create Client Account");
        return "external/registered-client/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String country,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {
        try {
            registeredClientService.registerClientAccount(firstName, lastName, email, phone, country, password);
            redirectAttributes.addFlashAttribute("successMessage", "Client account created. Please sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/client/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentPage", "client-dashboard");
        model.addAttribute("pageTitle", "Registered Client Dashboard");
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("orders", registeredClientService.ordersForUser(user));
        model.addAttribute("bookings", registeredClientService.bookingsForUser(user));
        model.addAttribute("donations", registeredClientService.donationsForUser(user));
        model.addAttribute("reviews", registeredClientService.reviewsForUser(user));
        model.addAttribute("savedItems", registeredClientService.savedItemsForUser(user));
        return "external/registered-client/dashboard";
    }

    @PostMapping("/client/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String country,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setCountry(country);
        registeredClientService.saveUserProfile(user);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/client/dashboard";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return registeredClientService.findUserByEmail(authentication.getName()).orElse(null);
    }
}
