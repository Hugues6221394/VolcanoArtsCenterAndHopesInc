package com.volcanoartscenter.platform.web.external.registeredclient.controller;

import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.CartItem;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.service.CaptchaService;
import com.volcanoartscenter.platform.shared.service.CartService;
import com.volcanoartscenter.platform.web.external.registeredclient.service.RegisteredClientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final RegisteredClientService registeredClientService;
    private final CaptchaService captchaService;
    private final CartService cartService;

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return registeredClientService.findUserByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        String anonSessionId = user == null ? session.getId() : null;
        Cart cart = cartService.getOrCreateCart(user, anonSessionId);
        
        Map<Product, Integer> cartItems = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            cartItems.put(p, item.getQuantity());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        model.addAttribute("currentPage", "cart");
        model.addAttribute("pageTitle", "Shopping Cart — Volcano Arts Center");
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        
        boolean isAuthenticated = user != null;
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            model.addAttribute("currentUser", user);
        }

        return "external/registered-client/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please sign in to add items to your cart.");
            return "redirect:/login?redirect=/art-store";
        }
        String anonSessionId = null;
        
        try {
            cartService.addItemToCart(user, anonSessionId, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to your cart.");
        } catch (PlatformException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not add item to cart.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        String anonSessionId = user == null ? session.getId() : null;
        cartService.removeItemFromCart(user, anonSessionId, productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkoutCart(@RequestParam String recipientName,
                               @RequestParam String recipientEmail,
                               @RequestParam(required = false) String recipientPhone,
                               @RequestParam String addressLine1,
                               @RequestParam(required = false) String addressLine2,
                               @RequestParam String city,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String postalCode,
                               @RequestParam String country,
                               @RequestParam(required = false) String paymentMethod,
                               @RequestParam(required = false) String captchaToken,
                               HttpSession session,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Captcha validation failed.");
            return "redirect:/cart";
        }

        User user = getAuthenticatedUser(authentication);
        String anonSessionId = user == null ? session.getId() : null;
        Cart cart = cartService.getOrCreateCart(user, anonSessionId);

        if (cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/cart";
        }

        if (user == null) {
            // Force login exactly as art-store requires if strict policy applies.
            redirectAttributes.addFlashAttribute("successMessage", "Please register or sign in to complete checkout.");
            return "redirect:/login";
        }

        try {
            registeredClientService.createShippingOrderFromCart(
                    cart,
                    user,
                    recipientName,
                    recipientEmail,
                    recipientPhone,
                    addressLine1,
                    addressLine2,
                    city,
                    state,
                    postalCode,
                    country,
                    paymentMethod
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }

        cartService.clearCart(user, anonSessionId);
        redirectAttributes.addFlashAttribute("successMessage", "Cart Checkout successful. Our team will contact you with details soon.");
        return "redirect:/client/dashboard";
    }
}
