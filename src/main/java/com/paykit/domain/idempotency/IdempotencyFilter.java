package com.paykit.domain.idempotency;

import com.paykit.common.AppConstants;
import com.paykit.exception.IdempotencyConflictException;
import com.paykit.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;

    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!IDEMPOTENT_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(AppConstants.IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Optional<IdempotencyLog> existing = idempotencyService.check(tenantId, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyLog log = existing.get();
            String requestHash = hashBody(wrappedRequest.getContentAsByteArray());

            if (requestHash.isEmpty()) {
                wrappedRequest.getInputStream().readAllBytes();
                requestHash = hashBody(wrappedRequest.getContentAsByteArray());
            }

            if (!log.getRequestHash().equals(requestHash) && !requestHash.isEmpty()) {
                throw new IdempotencyConflictException(idempotencyKey);
            }

            response.setStatus(log.getResponseStatus());
            response.setContentType("application/json");
            if (log.getResponseBody() != null) {
                response.getWriter().write(log.getResponseBody());
            }
            return;
        }

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        String requestHash = hashBody(wrappedRequest.getContentAsByteArray());
        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
        int responseStatus = wrappedResponse.getStatus();

        try {
            idempotencyService.save(tenantId, idempotencyKey, request.getRequestURI(),
                    requestHash, responseStatus, responseBody);
        } catch (Exception e) {
            logger.warn("Failed to save idempotency log for key: " + idempotencyKey, e);
        }

        wrappedResponse.copyBodyToResponse();
    }

    private String hashBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
