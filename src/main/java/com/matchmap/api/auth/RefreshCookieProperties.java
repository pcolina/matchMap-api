package com.matchmap.api.auth;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("matchmap.auth.refresh-cookie")
record RefreshCookieProperties(String sameSite) {

  private static final Set<String> ALLOWED_SAME_SITE = Set.of("Strict", "Lax", "None");

  RefreshCookieProperties {
    if (sameSite == null || !ALLOWED_SAME_SITE.contains(sameSite)) {
      throw new IllegalArgumentException(
          "MATCHMAP_REFRESH_COOKIE_SAMESITE must be Strict, Lax or None");
    }
  }
}
