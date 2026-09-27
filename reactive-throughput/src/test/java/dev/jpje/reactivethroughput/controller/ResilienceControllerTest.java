package dev.jpje.reactivethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Resilience Controller Test")
class ResilienceControllerTest {

  private static final String RESILIENCE_URL = "/resilience";
  private static final String OK_PREFIX = "OK:Reactive:Resilience:";
  private static final String FALLBACK_BODY = "FALLBACK:Reactive:Resilience:timeout";

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @Autowired
  private WebTestClient webTestClient;

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
  void shouldReturnOkWhenDownstreamAnswersInsideDeadline() {
    // Given
    this.stubDownstream(0);

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String.class)
      .value(body -> assertThat(body).startsWith(OK_PREFIX).contains("downstream-data-1"));

    // And the downstream call omits delayMs when the caller value is zero
    wireMock.verify(getRequestedFor(urlPathEqualTo("/api/data"))
      .withoutQueryParam("delayMs"));
  }

  @Test
  @DisplayName("Should forward the caller delayMs to the downstream")
  void shouldForwardDelayMsToDownstream() {
    // Given
    this.stubDownstream(0);

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL + "?delayMs=250")
      .exchange()
      .expectStatus().isOk();
    wireMock.verify(getRequestedFor(urlPathEqualTo("/api/data"))
      .withQueryParam("delayMs", equalTo("250")));
  }

  @Test
  @DisplayName("Should return the fixed fallback when the downstream exceeds the deadline")
  void shouldReturnFallbackWhenDownstreamExceedsDeadline() {
    // Given - a 1000 ms downstream delay is longer than the 500 ms deadline
    this.stubDownstream(1_000);

    // When, Then
    this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(5))
      .build()
      .get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .isEqualTo(FALLBACK_BODY);
  }

  @Test
  @DisplayName("Should return the fixed fallback when the downstream fails")
  void shouldReturnFallbackWhenDownstreamFails() {
    // Given
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse().withStatus(500)));

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .isEqualTo(FALLBACK_BODY);
  }

  @Test
  @DisplayName("Should complete via reactive pipeline without blocking event loop")
  void shouldCompleteViaReactivePipeline() {
    // Given
    this.stubDownstream(0);

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.startsWith(OK_PREFIX))
      .expectComplete()
      .verify(Duration.ofSeconds(5));
  }
}
