package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.notification.InAppNotificationService;
import com.volcanoartscenter.platform.shared.notification.NotificationCategory;
import com.volcanoartscenter.platform.shared.notification.NotificationDto;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import com.volcanoartscenter.platform.shared.web.api.CursorPage;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/notifications")
public class ClientNotificationApiController {

    private final InAppNotificationService notifications;
    private final UserRepository userRepository;

    public ClientNotificationApiController(InAppNotificationService notifications,
                                           UserRepository userRepository) {
        this.notifications = notifications;
        this.userRepository = userRepository;
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ApiResponse.ok(Map.of("unread", notifications.countUnread(userId)));
    }

    @GetMapping
    public ApiResponse<CursorPage<NotificationDto>> list(Authentication authentication,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) NotificationCategory category,
                                                          @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Long userId = currentUserId(authentication);
        Page<NotificationDto> p = notifications.inbox(userId, page, size, category, unreadOnly)
                .map(NotificationDto::from);
        String nextCursor = p.hasNext() ? String.valueOf(page + 1) : null;
        return ApiResponse.ok(CursorPage.of(p.getContent(), nextCursor, p.getSize(), p.getTotalElements()));
    }

    @GetMapping("/recent")
    public ApiResponse<List<NotificationDto>> recent(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return ApiResponse.ok(notifications.recent(userId).stream().map(NotificationDto::from).toList());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = currentUserId(authentication);
        boolean updated = notifications.markRead(userId, id);
        if (!updated) {
            throw new NotFoundException("Notification", id);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(Authentication authentication) {
        Long userId = currentUserId(authentication);
        int updated = notifications.markAllRead(userId);
        return ApiResponse.ok(Map.of("updated", updated));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new PlatformException("UNAUTHENTICATED", "Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new PlatformException("USER_NOT_FOUND", "Authenticated user record missing.", HttpStatus.UNAUTHORIZED));
    }
}
