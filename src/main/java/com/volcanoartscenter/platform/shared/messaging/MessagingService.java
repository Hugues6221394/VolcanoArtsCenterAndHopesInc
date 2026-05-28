package com.volcanoartscenter.platform.shared.messaging;

import com.volcanoartscenter.platform.shared.exception.BusinessRuleException;
import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.notification.NotificationCategory;
import com.volcanoartscenter.platform.shared.notification.NotificationEvent;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final List<String> STAFF_ROLES =
            List.of("SUPER_ADMIN", "CONTENT_MANAGER", "OPS_MANAGER");
    private static final int MAX_BODY_CHARS = 4000;
    private static final int MAX_SUBJECT_CHARS = 200;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public Conversation openConversation(User client, Long productId, String subject, String firstMessage) {
        if (client == null) {
            throw new BusinessRuleException("MESSAGING_AUTH_REQUIRED", "Sign in to message the gallery.");
        }
        String cleanSubject = sanitizeSubject(subject);
        String cleanBody = sanitizeBody(firstMessage);

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
            if (product != null && (cleanSubject == null || cleanSubject.isBlank())) {
                cleanSubject = "Question about: " + product.getName();
            }
        }
        if (cleanSubject == null || cleanSubject.isBlank()) {
            cleanSubject = "New inquiry";
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation.builder()
                .subject(cleanSubject)
                .status(ConversationStatus.AWAITING_STAFF)
                .openedByUserId(client.getId())
                .productId(product != null ? product.getId() : null)
                .lastMessageAt(now)
                .lastMessageByRole(MessageSenderRole.CLIENT)
                .build();
        Conversation saved = conversationRepository.save(conversation);

        Message message = Message.builder()
                .conversationId(saved.getId())
                .senderUserId(client.getId())
                .senderRole(MessageSenderRole.CLIENT)
                .body(cleanBody)
                .build();
        messageRepository.save(message);

        notifyStaff(NotificationCategory.STAFF_NEW_INQUIRY,
                "New inquiry: " + cleanSubject,
                client.getFullName() + ": " + preview(cleanBody),
                "/admin/messages/" + saved.getId(),
                saved.getId());

        return saved;
    }

    @Transactional
    public Message reply(User sender, Long conversationId, String body, MessageSenderRole role) {
        if (sender == null) throw new BusinessRuleException("MESSAGING_AUTH_REQUIRED", "Sign in required.");
        String cleanBody = sanitizeBody(body);

        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));

        if (role == MessageSenderRole.CLIENT && !c.getOpenedByUserId().equals(sender.getId())) {
            throw new BusinessRuleException("MESSAGING_FORBIDDEN", "You don't have access to this conversation.");
        }
        if (c.getStatus() == ConversationStatus.RESOLVED) {
            c.setStatus(role == MessageSenderRole.CLIENT
                    ? ConversationStatus.AWAITING_STAFF
                    : ConversationStatus.AWAITING_CLIENT);
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .conversationId(c.getId())
                .senderUserId(sender.getId())
                .senderRole(role)
                .body(cleanBody)
                .build();
        messageRepository.save(message);

        c.setLastMessageAt(now);
        c.setLastMessageByRole(role);
        c.setStatus(role == MessageSenderRole.CLIENT
                ? ConversationStatus.AWAITING_STAFF
                : ConversationStatus.AWAITING_CLIENT);
        if (role == MessageSenderRole.STAFF && c.getAssignedStaffUserId() == null) {
            c.setAssignedStaffUserId(sender.getId());
        }

        if (role == MessageSenderRole.CLIENT) {
            notifyStaff(NotificationCategory.STAFF_NEW_INQUIRY,
                    "New reply: " + c.getSubject(),
                    sender.getFullName() + ": " + preview(cleanBody),
                    "/admin/messages/" + c.getId(),
                    c.getId());
        } else {
            events.publishEvent(NotificationEvent.forEntity(
                    c.getOpenedByUserId(),
                    NotificationCategory.MESSAGE_RECEIVED,
                    "Reply from the gallery",
                    sender.getFullName() + ": " + preview(cleanBody),
                    "/account/messages/" + c.getId(),
                    "Conversation",
                    c.getId()));
        }

        return message;
    }

    @Transactional
    public int markThreadReadFor(Long conversationId, MessageSenderRole viewerRole) {
        return messageRepository.markIncomingRead(conversationId, viewerRole, LocalDateTime.now());
    }

    @Transactional
    public void resolve(Long conversationId, User staff) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
        c.setStatus(ConversationStatus.RESOLVED);
        if (c.getAssignedStaffUserId() == null && staff != null) {
            c.setAssignedStaffUserId(staff.getId());
        }
    }

    @Transactional(readOnly = true)
    public Page<Conversation> inboxForUser(Long userId, int page, int size) {
        return conversationRepository.findByOpenedByUserIdOrderByLastMessageAtDesc(
                userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Conversation> inboxForStaff(boolean openOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (openOnly) {
            return conversationRepository.findByStatusInOrderByLastMessageAtDesc(
                    List.of(ConversationStatus.OPEN, ConversationStatus.AWAITING_STAFF, ConversationStatus.AWAITING_CLIENT),
                    pageable);
        }
        return conversationRepository.findAllByOrderByLastMessageAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }

    @Transactional(readOnly = true)
    public List<Message> messagesFor(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional(readOnly = true)
    public long countUnreadThreadsForClient(Long userId) {
        return conversationRepository.countThreadsWithUnreadStaffRepliesFor(userId);
    }

    @Transactional(readOnly = true)
    public long countAwaitingStaffThreads() {
        return conversationRepository.countAwaitingStaff();
    }

    private void notifyStaff(NotificationCategory category, String title, String body, String ctaUrl, Long convId) {
        List<User> staff = userRepository.findByRoles_NameIn(STAFF_ROLES);
        for (User s : staff) {
            events.publishEvent(NotificationEvent.forEntity(
                    s.getId(), category, title, body, ctaUrl, "Conversation", convId));
        }
    }

    private String sanitizeSubject(String raw) {
        if (raw == null) return null;
        String trimmed = raw.replaceAll("[\\r\\n]+", " ").trim();
        if (trimmed.length() > MAX_SUBJECT_CHARS) trimmed = trimmed.substring(0, MAX_SUBJECT_CHARS);
        return trimmed;
    }

    private String sanitizeBody(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleException("MESSAGING_EMPTY_BODY", "Message body is required.");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_BODY_CHARS) trimmed = trimmed.substring(0, MAX_BODY_CHARS);
        return trimmed;
    }

    private String preview(String body) {
        if (body == null) return "";
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) + "…" : oneLine;
    }
}
