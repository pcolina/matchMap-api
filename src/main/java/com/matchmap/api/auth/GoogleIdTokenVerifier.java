package com.matchmap.api.auth;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
class GoogleIdTokenVerifier {

  private final NimbusJwtDecoder decoder;

  GoogleIdTokenVerifier(JWKSource<SecurityContext> googleJwkSource) {
    this.decoder =
        NimbusJwtDecoder.withJwkSource(googleJwkSource)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
  }

  GoogleIdentity verify(String idToken) {
    var jwt = decoder.decode(idToken);
    return new GoogleIdentity(
        jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
  }
}
