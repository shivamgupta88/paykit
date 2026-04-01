package com.paykit.domain.auth;

import com.paykit.domain.auth.dto.AuthResponse;
import com.paykit.domain.auth.dto.LoginRequest;
import com.paykit.domain.auth.dto.RegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public AuthResponse register(RegisterRequest request) {
        throw new UnsupportedOperationException("Requires Phase 3: User entity");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        throw new UnsupportedOperationException("Requires Phase 3: User entity");
    }
}
