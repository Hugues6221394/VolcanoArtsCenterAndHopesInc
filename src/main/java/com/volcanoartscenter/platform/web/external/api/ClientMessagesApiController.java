package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.messaging.MessagingService;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client/messages")
public class ClientMessagesApiController {

    private static final Set<String> STAFF_ROLES = Set.of("SUPER_ADMIN", "CONTENT_MANAGER", "OPS_MANAGER");

    private final MessagingService messagingService;
    private final UserRepository userRepository;

    public ClientMessagesApiController(MessagingService messagingService, UserRepository userRepository) {
        this.messagingService = messagingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        Set<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
        long unread = roles.stream().anyMatch(STAFF_ROLES::contains)
                ? messagingService.countAwaitingStaffThreads()
                : messagingService.countUnreadThreadsForClient(user.getId());
        return ApiResponse.ok(Map.of("unread", unread));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new PlatformException("UNAUTHENTICATED", "Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new PlatformException("USER_NOT_FOUND", "Authenticated user record missing.", HttpStatus.UNAUTHORIZED));
    }
}
