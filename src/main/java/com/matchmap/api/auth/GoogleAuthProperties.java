package com.matchmap.api.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("matchmap.auth.google")
record GoogleAuthProperties(List<String> clientIds) {}
