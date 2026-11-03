package com.matchmap.api.auth;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import java.net.MalformedURLException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration(proxyBeanMethods = false)
@EnableResilientMethods(proxyTargetClass = true)
class GoogleJwkConfiguration {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 3_000;

  @Bean
  JWKSource<SecurityContext> googleJwkSource() throws MalformedURLException {
    var url = URI.create("https://www.googleapis.com/oauth2/v3/certs").toURL();
    return new ResilientJwkSource(
        JWKSourceBuilder.<SecurityContext>create(
                url, new DefaultResourceRetriever(CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS))
            .build());
  }
}
