package edu.ignacio.poc.imperativethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import edu.ignacio.poc.imperativethroughput.config.MetricsConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
@WebMvcTest({ResilienceController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@Import({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class, MetricsConfiguration.class})
@DisplayName("Resilience Controller Test")
class ResilienceControllerTest {

  private static final String RESILIENCE_URL = "/resilience";

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ResilienceController controller;

  @DynamicPropertySource
  private static void configureDownstreamUrl(final DynamicPropertyRegistry registry) {
    registry.add("downstream.service.url", wireMock::baseUrl);
  }

  @BeforeEach
  void resetStubs() {
    wireMock.resetAll();
  }

  private static String dynamicDownstreamBody() {
    return """
      {
        "id": "%s",
        "value": "downstream-data-%s",
        "timestamp": "%s"
      }
      """.formatted(UUID.randomUUID(), UUID.randomUUID().toString().substring(0, 8),
      java.time.Instant.now().toString());
  }

  @Test
  @DisplayName("Should return OK with downstream data when upstream responds in time")
  void shouldReturnOkWhenDownstreamRespondsInTime() throws Exception {
    // Given
    final var downstreamBody = dynamicDownstreamBody();
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(downstreamBody)));

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("OK:Imperative:Resilience:")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("downstream-data-")));
  }

  @Test
  @DisplayName("Should return fallback when downstream exceeds timeout")
  void shouldReturnFallbackOnTimeout() throws Exception {
    // Given - simulate a slow downstream (1000ms > 500ms timeout)
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(1_000)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then - should return fallback, not an error
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("FALLBACK:Imperative:Resilience:timeout")));
  }

  @Test
  @DisplayName("Should return fallback when delayMs exceeds timeout")
  void shouldReturnFallbackWhenDelayMsExceedsTimeout() throws Exception {
    // Given - delayMs=1000ms causes the virtual thread to sleep beyond the 500ms timeout
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL).param("delayMs", "1000"))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("FALLBACK:Imperative:Resilience:timeout")));
  }

  @Test
  @DisplayName("Should complete future without blocking caller thread")
  void shouldCompleteFutureWithoutBlockingCallerThread() throws Exception {
    // Given
    final var downstreamBody = dynamicDownstreamBody();
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(downstreamBody)));

    // When
    final var future = this.controller.getResilience(0L);
    assertThat(future).isNotNull();

    // Then
    final var response = future.get(3, TimeUnit.SECONDS);
    assertThat(response)
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().contains("downstream-data-"));
  }
}
