package com.example.lotto.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("lotto.security")
public record SecurityProperties(
        String aesKey,
        String hmacKey,
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        int maxFailedLogins,
        Duration lockDuration,
        boolean secureCookie) {
}
