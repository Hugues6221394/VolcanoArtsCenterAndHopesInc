package com.volcanoartscenter.platform.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcanoartscenter.platform.shared.web.api.ApiError;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Component
@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final IdempotencyStore store;
    private final IdempotencyProperties props;
    private final ObjectMapper mapper;

    public IdempotencyFilter(IdempotencyStore store, IdempotencyProperties props, ObjectMapper mapper) {
        this.store = store;
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) return true;
        if (!WRITE_METHODS.contains(request.getMethod())) return true;
        String path = request.getRequestURI();
        return props.getProtectedPaths().stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            writeJson(response, HttpStatus.BAD_REQUEST, ApiResponse.failure(
                    ApiError.of("IDEMPOTENCY_KEY_REQUIRED",
                            "Idempotency-Key header is required for this operation.")));
            return;
        }

        String storeKey = request.getMethod() + ":" + request.getRequestURI() + ":" + key;

        Optional<IdempotencyStore.CachedResponse> existing = store.read(storeKey);
        if (existing.isPresent()) {
            replay(response, existing.get());
            return;
        }

        if (!store.acquire(storeKey)) {
            if (store.isProcessing(storeKey)) {
                writeJson(response, HttpStatus.CONFLICT, ApiResponse.failure(
                        ApiError.of("IDEMPOTENCY_IN_PROGRESS",
                                "A request with this Idempotency-Key is already being processed.")));
                return;
            }
            existing = store.read(storeKey);
            if (existing.isPresent()) {
                replay(response, existing.get());
                return;
            }
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);

            if (wrapper.getStatus() >= 200 && wrapper.getStatus() < 500) {
                byte[] body = wrapper.getContentAsByteArray();
                String contentType = wrapper.getContentType();
                store.store(storeKey, new IdempotencyStore.CachedResponse(
                        wrapper.getStatus(),
                        contentType,
                        body.length == 0 ? null : new String(body, StandardCharsets.UTF_8)));
            } else {
                store.release(storeKey);
            }
            wrapper.copyBodyToResponse();
        } catch (RuntimeException ex) {
            store.release(storeKey);
            throw ex;
        }
    }

    private void replay(HttpServletResponse response, IdempotencyStore.CachedResponse cached) throws IOException {
        response.setStatus(cached.status());
        if (cached.contentType() != null) {
            response.setContentType(cached.contentType());
        }
        response.setHeader("Idempotent-Replay", "true");
        if (cached.body() != null) {
            byte[] bytes = cached.body().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
        response.getOutputStream().flush();
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] bytes = mapper.writeValueAsBytes(body);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
