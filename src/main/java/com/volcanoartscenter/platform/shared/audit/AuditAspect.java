package com.volcanoartscenter.platform.shared.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(com.volcanoartscenter.platform.shared.audit.Audited)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Audited annotation = method.getAnnotation(Audited.class);

        Object result = pjp.proceed();

        Map<String, Object> details = new HashMap<>();
        details.put("method", sig.getDeclaringTypeName() + "." + method.getName());
        details.put("args", safeArgs(pjp.getArgs()));

        Long entityId = extractEntityId(result, pjp.getArgs());
        auditLogService.record(annotation.action(), annotation.entityType(), entityId, details);
        return result;
    }

    private Object safeArgs(Object[] args) {
        return Arrays.stream(args)
                .map(a -> a == null ? null : a.getClass().getSimpleName() + "@" + System.identityHashCode(a))
                .toList();
    }

    private Long extractEntityId(Object result, Object[] args) {
        if (result instanceof Number n) return n.longValue();
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
        }
        return null;
    }
}
