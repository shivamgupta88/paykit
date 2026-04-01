package com.paykit.domain.auth;

import com.paykit.domain.auth.dto.AuthResponse;
import com.paykit.domain.auth.dto.LoginRequest;
import com.paykit.domain.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
