package com.paykit.domain.auth;

import com.paykit.domain.auth.dto.AuthResponse;
import com.paykit.domain.auth.dto.LoginRequest;
import com.paykit.domain.auth.dto.RegisterRequest;
import com.paykit.domain.tenant.Tenant;
import com.paykit.domain.tenant.TenantRepository;
import com.paykit.domain.tenant.TenantStatus;
import com.paykit.domain.user.User;
import com.paykit.domain.user.UserRepository;
import com.paykit.domain.user.UserRole;
import com.paykit.domain.user.UserStatus;
import com.paykit.exception.DuplicateResourceException;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.security.JwtService;
import com.paykit.tenant.TenantContext;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    @Timed(value = "paykit.auth.register", description = "Time to register a user")
    public AuthResponse register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.getTenantId()));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is not active");
        }

        if (userRepository.existsByTenantIdAndEmail(request.getTenantId(), request.getEmail())) {
            throw new DuplicateResourceException("Email already registered for this tenant");
        }

        TenantContext.set(request.getTenantId());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(), tenant.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .tenantId(tenant.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "paykit.auth.login", description = "Time to login a user")
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByTenantIdAndEmail(request.getTenantId(), request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is disabled");
        }

        String token = jwtService.generateToken(
                user.getId(), request.getTenantId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .tenantId(request.getTenantId())
                .build();
    }
}
