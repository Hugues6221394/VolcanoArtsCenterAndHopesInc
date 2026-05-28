package com.volcanoartscenter.platform.shared.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcanoartscenter.platform.shared.model.AuditEvent;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.AuditEventRepository;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditEventRepository auditRepo;
    private final UserRepository userRepo;
    private final ObjectMapper mapper;

    public AuditLogService(AuditEventRepository auditRepo, UserRepository userRepo, ObjectMapper mapper) {
        this.auditRepo = auditRepo;
        this.userRepo = userRepo;
        this.mapper = mapper;
    }

    @Async
    public void record(String action, String entityType, Long entityId,
                       Object oldValue, Object newValue, Map<String, Object> details) {
        try {
            ActorContext actor = currentActor();
            RequestContext req = currentRequest();
            AuditEvent event = AuditEvent.builder()
                    .actorUserId(actor.userId)
                    .actorEmail(actor.email)
                    .actorRole(actor.role)
                    .eventType(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .details(toJson(details))
                    .ipAddress(req.ipAddress)
                    .userAgent(req.userAgent)
                    .requestId(MDC.get("requestId"))
                    .build();
            auditRepo.save(event);
        } catch (Exception ex) {
            log.error("Failed to write audit event action={} entity={}#{}", action, entityType, entityId, ex);
        }
    }

    @Async
    public void record(String action, String entityType, Long entityId, Map<String, Object> details) {
        record(action, entityType, entityId, null, null, details);
    }

    private ActorContext currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return new ActorContext(null, null, "GUEST");
        }
        String email = auth.getName();
        Long id = userRepo.findByEmail(email).map(User::getId).orElse(null);
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
        return new ActorContext(id, email, role);
    }

    private RequestContext currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return new RequestContext(null, null);
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
            else ip = ip.split(",")[0].trim();
            return new RequestContext(ip, req.getHeader("User-Agent"));
        } catch (IllegalStateException ex) {
            return new RequestContext(null, null);
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private record ActorContext(Long userId, String email, String role) {}
    private record RequestContext(String ipAddress, String userAgent) {}
}
