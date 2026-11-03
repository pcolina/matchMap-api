package com.matchmap.api.auth;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.List;
import org.springframework.resilience.annotation.Retryable;

class ResilientJwkSource implements JWKSource<SecurityContext> {

  private final JWKSource<SecurityContext> delegate;

  ResilientJwkSource(JWKSource<SecurityContext> delegate) {
    this.delegate = delegate;
  }

  @Override
  @Retryable(maxRetries = 2, delay = 500, multiplier = 2, includes = KeySourceException.class)
  public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
    return delegate.get(selector, context);
  }
}
