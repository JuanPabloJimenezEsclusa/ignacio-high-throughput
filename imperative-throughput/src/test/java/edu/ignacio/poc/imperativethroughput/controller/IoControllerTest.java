package edu.ignacio.poc.imperativethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import edu.ignacio.poc.imperativethroughput.config.MetricsConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@WebMvcTest({IoController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@Import({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class, MetricsConfiguration.class})
@DisplayName("IO Controller Test")
class IoControllerTest {

  private static final String IO_URL = "/io";
  private static final String DOWNSTREAM_BODY = "downstream-data";
  private static final String EXPECTED_BODY = "OK:Imperative:IO:" + DOWNSTREAM_BODY;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IoController controller;

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @DynamicPropertySource
  private static void configureDownstreamUrl(final DynamicPropertyRegistry registry) {
    registry.add("downstream.service.url", wireMock::baseUrl);
  }

  @Test
  @DisplayName("Should return OK with downstream data via blocking RestClient on virtual thread")
  void shouldReturnOkWithDownstreamData() throws Exception {
    // Given
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
        .withBody(DOWNSTREAM_BODY)));

    // When
    final var mvcResult = this.mockMvc.perform(get(IO_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString(EXPECTED_BODY)));
  }

  @Test
  @DisplayName("Should complete future without blocking caller thread")
  void shouldCompleteFutureWithoutBlockingCallerThread() throws Exception {
    // Given
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(DOWNSTREAM_BODY)));

    // When
    final var future = this.controller.getIo();
    assertThat(future).isNotNull();

    // Then
    final var response = future.get(3, TimeUnit.SECONDS);
    assertThat(response)
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().contains(EXPECTED_BODY));
  }

  @Test
  @DisplayName("Should include downstream response body in result")
  void shouldIncludeDownstreamBodyInResult() throws Exception {
    // Given
    final var customBody = "custom-response-42";
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(customBody)));

    // When
    final var response = this.controller.getIo().get(3, TimeUnit.SECONDS);

    // Then
    assertThat(response.getBody()).contains(customBody);
    wireMock.verify(1, WireMock.getRequestedFor(urlEqualTo("/api/data")));
  }
}
