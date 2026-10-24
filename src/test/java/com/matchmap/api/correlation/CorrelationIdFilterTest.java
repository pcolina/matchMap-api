package com.matchmap.api.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private static final String HEADER = "X-Correlation-Id";

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void reusesClientId() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(HEADER, "3f2b8c1e-7a4d-4e2b-9c11-5d6e7f8a9b0c");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(HEADER)).isEqualTo("3f2b8c1e-7a4d-4e2b-9c11-5d6e7f8a9b0c");
  }

  @Test
  void generatesUuidWhenAbsent() throws Exception {
    String correlationId = filterWithoutHeader();

    assertThat(correlationId).isNotNull();
    assertThat(UUID.fromString(correlationId)).hasToString(correlationId);
  }

  @Test
  void generatesDifferentIdsPerRequest() throws Exception {
    String first = filterWithoutHeader();
    String second = filterWithoutHeader();

    assertThat(first).isNotNull().isNotEqualTo(second);
  }

  private String filterWithoutHeader() throws Exception {
    var response = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
    return response.getHeader(HEADER);
  }
}
