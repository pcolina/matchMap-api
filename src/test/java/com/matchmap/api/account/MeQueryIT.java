package com.matchmap.api.account;

import static com.matchmap.api.support.GraphqlRequests.body;
import static com.matchmap.api.support.GraphqlRequests.registerBody;
import static com.matchmap.api.support.GraphqlRequests.uniqueSuffix;
import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MeQueryIT {

  private static final String PASSWORD = "Correcthorse1!";
  private static final String ME = "{ me { id email username } }";
  private static final String ME_ALL_FIELDS = "{ me { id email username isFirstVisit } }";
  private static final String ME_FIRST_VISIT = "{ me { isFirstVisit } }";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvcTester mockMvc;

  @Autowired JdbcTemplate jdbcTemplate;

  @Value("${matchmap.auth.jwt-secret}")
  String jwtSecret;

  @Test
  void meReturnsIdEmailAndUsername() throws Exception {
    String suffix = uniqueSuffix();
    String email = "mia" + suffix + "@example.com";
    var registered = graphql(registerBody(email, PASSWORD), null);
    String id = data(registered).at("/register/id").asString();

    var response = graphql(body(ME_ALL_FIELDS), login(email));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response).bodyJson().extractingPath("$.data.me.id").isEqualTo(id);
    assertThat(response).bodyJson().extractingPath("$.data.me.email").isEqualTo(email);
    assertThat(response).bodyJson().extractingPath("$.data.me.username").isEqualTo("mia" + suffix);
    assertThat(response).bodyJson().extractingPath("$.data.me.isFirstVisit").isEqualTo(true);
  }

  @Test
  void meReturnsEmptyProfileAndFreePlan() throws Exception {
    String email = "nora" + uniqueSuffix() + "@example.com";
    registerAccount(email);

    var response =
        graphql(
            body("{ me { firstName lastName subscription nationality birthCountry } }"),
            login(email));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response).bodyJson().extractingPath("$.data.me.firstName").isNull();
    assertThat(response).bodyJson().extractingPath("$.data.me.lastName").isNull();
    assertThat(response).bodyJson().extractingPath("$.data.me.nationality").isNull();
    assertThat(response).bodyJson().extractingPath("$.data.me.birthCountry").isNull();
    assertThat(response).bodyJson().extractingPath("$.data.me.subscription").isEqualTo("FREE");
  }

  @Test
  void storedProfileValuesAppearInMe() throws Exception {
    String email = "olga" + uniqueSuffix() + "@example.com";
    registerAccount(email);
    jdbcTemplate.update(
        "UPDATE user_account SET first_name = ?, last_name = ?, nationality = ?, birth_country = ?"
            + " WHERE email = ?",
        "Ana",
        "Pérez",
        "ES",
        "FR",
        email);

    var response =
        graphql(body("{ me { firstName lastName nationality birthCountry } }"), login(email));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response).bodyJson().extractingPath("$.data.me.firstName").isEqualTo("Ana");
    assertThat(response).bodyJson().extractingPath("$.data.me.lastName").isEqualTo("Pérez");
    assertThat(response).bodyJson().extractingPath("$.data.me.nationality").isEqualTo("ES");
    assertThat(response).bodyJson().extractingPath("$.data.me.birthCountry").isEqualTo("FR");
  }

  @Test
  void passwordHashIsNotQueryable() throws Exception {
    String email = "pia" + uniqueSuffix() + "@example.com";
    registerAccount(email);

    var response = graphql(body("{ me { passwordHash } }"), login(email));

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("ValidationError");
  }

  @Test
  void signedInUserReadsFirstVisitTrue() throws Exception {
    String suffix = uniqueSuffix();
    String email = "vera" + suffix + "@example.com";
    registerAccount(email);

    var response = graphql(body(ME_FIRST_VISIT), login(email));

    assertThat(response).bodyJson().doesNotHavePath("$.errors");
    assertThat(response).bodyJson().extractingPath("$.data.me.isFirstVisit").isEqualTo(true);
  }

  @Test
  void anonymousClientIsUnauthorized() {
    var response = graphql(body(ME), null);

    assertThat(response)
        .bodyJson()
        .extractingPath("$.errors[0].extensions.classification")
        .isEqualTo("UNAUTHORIZED");
  }

  @Test
  void tamperedTokenIsRejected() throws Exception {
    String suffix = uniqueSuffix();
    String email = "tom" + suffix + "@example.com";
    registerAccount(email);
    var parts = login(email).split("\\.");
    var payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    var tamperedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                payload.replace("\"sub\":\"", "\"sub\":\"x").getBytes(StandardCharsets.UTF_8));
    var tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

    var response = graphql(body(ME), tampered);

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
  }

  @Test
  void expiredTokenIsRejected() {
    var now = Instant.now();
    var expired = signed(now.minusSeconds(3600), now.minusSeconds(1800));

    var response = graphql(body(ME), expired);

    assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(response.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
  }

  private String signed(Instant issuedAt, Instant expiresAt) {
    var key = new SecretKeySpec(Base64.getDecoder().decode(jwtSecret), "HmacSHA256");
    var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    var claims =
        JwtClaimsSet.builder()
            .issuer("matchmap-api")
            .subject(UUID.randomUUID().toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    var header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private void registerAccount(String email) {
    graphql(registerBody(email, PASSWORD), null);
  }

  private String login(String email) throws Exception {
    var response =
        mockMvc
            .post()
            .uri("/api/auth/login")
            .header("X-Client", "mobile")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(Map.of("email", email, "password", PASSWORD)))
            .exchange();
    return JSON.readTree(response.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private JsonNode data(MvcTestResult response) throws Exception {
    return JSON.readTree(response.getResponse().getContentAsString()).get("data");
  }

  private MvcTestResult graphql(String requestBody, String accessToken) {
    var request =
        mockMvc
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
    if (accessToken != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
    return request.content(requestBody).exchange();
  }
}
