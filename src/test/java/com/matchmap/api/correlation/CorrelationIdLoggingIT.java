package com.matchmap.api.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchmap.api.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, CorrelationIdLoggingIT.ProbeConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CorrelationIdLoggingIT {

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdLoggingIT.class);

  @Autowired MockMvcTester mockMvc;

  @Test
  void logLinesContainId(CapturedOutput output) {
    mockMvc
        .get()
        .uri("/test/log-probe")
        .param("marker", "probe-one")
        .header("X-Correlation-Id", "test-correlation-1")
        .exchange();

    assertThat(linesWith(output, "probe-one"))
        .singleElement()
        .asString()
        .contains("test-correlation-1");
  }

  @Test
  void idDoesNotLeakToNextRequest(CapturedOutput output) {
    mockMvc
        .get()
        .uri("/test/log-probe")
        .param("marker", "first-request")
        .header("X-Correlation-Id", "test-correlation-1")
        .exchange();
    log.info("between-requests");
    mockMvc.get().uri("/test/log-probe").param("marker", "second-request").exchange();

    assertThat(linesWith(output, "between-requests"))
        .singleElement()
        .asString()
        .doesNotContain("test-correlation-1");
    assertThat(linesWith(output, "second-request"))
        .singleElement()
        .asString()
        .doesNotContain("test-correlation-1");
  }

  @Test
  void invalidIdIsNotLogged(CapturedOutput output) {
    mockMvc
        .get()
        .uri("/test/log-probe")
        .param("marker", "invalid-id")
        .header("X-Correlation-Id", "abc\nforged-log-line")
        .exchange();

    assertThat(linesWith(output, "invalid-id")).hasSize(1);
    assertThat(output.getAll()).doesNotContain("forged-log-line");
  }

  private static List<String> linesWith(CapturedOutput output, String marker) {
    return output.getAll().lines().filter(line -> line.contains(marker)).toList();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ProbeConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain probeSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.securityMatcher("/test/**")
          .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
          .build();
    }

    @RestController
    static class LogProbeController {

      private static final Logger probeLog = LoggerFactory.getLogger(LogProbeController.class);

      @GetMapping("/test/log-probe")
      void logProbe(@RequestParam String marker) {
        probeLog.info("log probe {}", marker);
      }
    }
  }
}
