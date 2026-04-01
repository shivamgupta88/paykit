package com.paykit.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "paykit.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs;
}
