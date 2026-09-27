package dev.jpje.imperativethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.jpje.imperativethroughput.ImperativeThroughputApplication;
import dev.jpje.imperativethroughput.config.MetricsConfiguration;
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
  private static final String OK_PREFIX = "OK:Imperative:Resilience:";
  private static final String FALLBACK_BODY = "FALLBACK:Imperative:Resilience:timeout";

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

  private void stubDownstream(final int fixedDelayMs) {
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(fixedDelayMs)
        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
        .withBody("downstream-data-1")));
  }

  @Test
  @DisplayName("Should return OK with the downstream body when downstream answers inside the deadline")
  void shouldReturnOkWhenDownstreamAnswersInsideDeadline() throws Exception {
    // Given
    this.stubDownstream(0);

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL).param("delayMs", "0"))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(OK_PREFIX)))
      .andExpect(content().string(containsString("downstream-data-1")));

    wireMock.verify(getRequestedFor(urlPathEqualTo("/api/data"))
      .withoutQueryParam("delayMs"));
  }

  @Test
  @DisplayName("Should forward the caller delayMs to the downstream")
  void shouldForwardDelayMsToDownstream() throws Exception {
    // Given
    this.stubDownstream(0);

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL).param("delayMs", "250"))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());
    wireMock.verify(getRequestedFor(urlPathEqualTo("/api/data"))
      .withQueryParam("delayMs", equalTo("250")));
  }

  @Test
  @DisplayName("Should return the fixed fallback when the downstream exceeds the deadline")
  void shouldReturnFallbackWhenDownstreamExceedsDeadline() throws Exception {
    // Given - a 1000 ms downstream delay is longer than the 500 ms deadline
    this.stubDownstream(1_000);

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(FALLBACK_BODY));
  }

  @Test
  @DisplayName("Should return the fixed fallback when the downstream fails")
  void shouldReturnFallbackWhenDownstreamFails() throws Exception {
    // Given
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse().withStatus(500)));

    // When
    final var mvcResult = this.mockMvc.perform(get(RESILIENCE_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(FALLBACK_BODY));
  }

  @Test
  @DisplayName("Should complete the future with the reserved OK body")
  void shouldCompleteFutureWithOkBody() throws Exception {
    // Given
    this.stubDownstream(0);

    // When
    final var future = this.controller.getResilience(0L);
    assertThat(future).as("resilience future must be returned").isNotNull();

    // Then
    final var response = future.get(3, TimeUnit.SECONDS);
    assertThat(response)
      .as("resilience response must be OK with the downstream body")
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().startsWith(OK_PREFIX));
  }
}
