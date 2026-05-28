package com.volcanoartscenter.platform.web.internal.messages;

import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.messaging.Conversation;
import com.volcanoartscenter.platform.shared.messaging.Message;
import com.volcanoartscenter.platform.shared.messaging.MessageSenderRole;
import com.volcanoartscenter.platform.shared.messaging.MessagingService;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AdminMessagesController {

    private final MessagingService messaging;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AdminMessagesController(MessagingService messaging,
                                   UserRepository userRepository,
                                   ProductRepository productRepository) {
        this.messaging = messaging;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/admin/messages")
    public String inbox(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "true") boolean openOnly,
                        Model model) {
        var pageData = messaging.inboxForStaff(openOnly, page, 25);

        Map<Long, String> openerNames = openerNamesFor(pageData.getContent());
        Map<Long, String> productNames = productNamesFor(pageData.getContent());

        model.addAttribute("currentPage", "admin-messages");
        model.addAttribute("adminPage", "messages");
        model.addAttribute("pageTitle", "Inbox — Admin");
        model.addAttribute("conversations", pageData.getContent());
        model.addAttribute("openerNames", openerNames);
        model.addAttribute("productNames", productNames);
        model.addAttribute("page", page);
        model.addAttribute("openOnly", openOnly);
        model.addAttribute("hasNext", pageData.hasNext());
        model.addAttribute("hasPrev", pageData.hasPrevious());
        model.addAttribute("awaitingStaffCount", messaging.countAwaitingStaffThreads());
        return "admin/messages/inbox";
    }

    @GetMapping("/admin/messages/{id}")
    public String thread(@PathVariable Long id, Model model) {
        Conversation c = messaging.getConversation(id);
        messaging.markThreadReadFor(id, MessageSenderRole.STAFF);
        List<Message> messages = messaging.messagesFor(id);
        Map<Long, String> senderNames = senderNamesFor(messages, c);
        Product product = c.getProductId() != null
                ? productRepository.findById(c.getProductId()).orElse(null) : null;
        User opener = userRepository.findById(c.getOpenedByUserId()).orElse(null);

        model.addAttribute("currentPage", "admin-messages");
        model.addAttribute("adminPage", "messages");
        model.addAttribute("pageTitle", c.getSubject() + " — Admin");
        model.addAttribute("conversation", c);
        model.addAttribute("messages", messages);
        model.addAttribute("senderNames", senderNames);
        model.addAttribute("product", product);
        model.addAttribute("opener", opener);
        return "admin/messages/thread";
    }

    @PostMapping("/admin/messages/{id}/reply")
    public String reply(Authentication auth, @PathVariable Long id,
                        @RequestParam String body, RedirectAttributes redirect) {
        User staff = staffFor(auth);
        if (staff == null) return "redirect:/admin/messages";
        try {
            messaging.reply(staff, id, body, MessageSenderRole.STAFF);
        } catch (PlatformException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/messages/" + id;
    }

    @PostMapping("/admin/messages/{id}/resolve")
    public String resolve(Authentication auth, @PathVariable Long id) {
        User staff = staffFor(auth);
        messaging.resolve(id, staff);
        return "redirect:/admin/messages/" + id;
    }

    private User staffFor(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName())) return null;
        return userRepository.findByEmail(a.getName()).orElse(null);
    }

    private Map<Long, String> openerNamesFor(List<Conversation> convs) {
        List<Long> ids = convs.stream().map(Conversation::getOpenedByUserId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private Map<Long, String> productNamesFor(List<Conversation> convs) {
        List<Long> ids = convs.stream()
                .map(Conversation::getProductId).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    private Map<Long, String> senderNamesFor(List<Message> messages, Conversation c) {
        Map<Long, String> map = new HashMap<>();
        for (Message m : messages) {
            map.computeIfAbsent(m.getSenderUserId(),
                    uid -> userRepository.findById(uid).map(User::getFullName).orElse("Unknown"));
        }
        return map;
    }
}
