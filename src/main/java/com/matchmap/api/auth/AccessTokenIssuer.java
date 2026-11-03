package com.matchmap.api.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
class AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final Duration ttl;

  AccessTokenIssuer(AuthProperties properties) {
    var key = new SecretKeySpec(Base64.getDecoder().decode(properties.jwtSecret()), "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    this.ttl = properties.accessTokenTtl();
  }

  Duration ttl() {
    return ttl;
  }

  String issue(UUID accountId) {
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("matchmap-api")
            .subject(accountId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .id(UUID.randomUUID().toString())
            .build();
    var header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
