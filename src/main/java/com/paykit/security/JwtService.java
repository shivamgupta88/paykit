package com.paykit.security;

import java.util.UUID;

public interface JwtService {

    String generateToken(UUID userId, UUID tenantId, String email, String role);

    boolean validateToken(String token);

    UUID extractUserId(String token);

    UUID extractTenantId(String token);

    String extractEmail(String token);

    String extractRole(String token);
}
