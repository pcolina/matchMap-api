package com.matchmap.api.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("matchmap.auth")
record AuthProperties(String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl) {}
