package com.matchmap.api.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class LoginThrottle {

  private final int maxFailures;
  private final Duration window;
  private final Map<String, Failures> failures = new ConcurrentHashMap<>();

  LoginThrottle(AuthProperties properties) {
    this.maxFailures = properties.maxFailedLogins();
    this.window = properties.failedLoginWindow();
  }

  Optional<Duration> retryAfter(String key, Instant now) {
    return Optional.ofNullable(failures.get(key))
        .filter(current -> now.isBefore(current.windowStart().plus(window)))
        .filter(current -> current.count() >= maxFailures)
        .map(current -> Duration.between(now, current.windowStart().plus(window)));
  }

  void recordFailure(String key, Instant now) {
    failures.compute(
        key,
        (ignored, current) ->
            current == null || !now.isBefore(current.windowStart().plus(window))
                ? new Failures(now, 1)
                : new Failures(current.windowStart(), current.count() + 1));
  }

  void reset(String key) {
    failures.remove(key);
  }

  private record Failures(Instant windowStart, int count) {}
}
