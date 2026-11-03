package com.matchmap.api.auth;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
class GoogleIdTokenVerifier {

  private static final Set<String> ISSUERS =
      Set.of("accounts.google.com", "https://accounts.google.com");

  private final NimbusJwtDecoder decoder;

  GoogleIdTokenVerifier(
      JWKSource<SecurityContext> googleJwkSource, GoogleAuthProperties properties) {
    var clientIds =
        properties.clientIds().stream()
            .filter(id -> !id.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    var decoder =
        NimbusJwtDecoder.withJwkSource(googleJwkSource)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
    OAuth2TokenValidator<Jwt> issuer =
        token ->
            ISSUERS.contains(token.getClaimAsString(JwtClaimNames.ISS))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    OAuth2TokenValidator<Jwt> audience =
        new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            audiences -> audiences != null && audiences.stream().anyMatch(clientIds::contains));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            List.of(new JwtTimestampValidator(), issuer, audience)));
    this.decoder = decoder;
  }

  GoogleIdentity verify(String idToken) {
    var jwt = decoder.decode(idToken);
    return new GoogleIdentity(
        jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
  }
}
