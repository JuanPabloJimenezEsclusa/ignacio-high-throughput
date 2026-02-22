package edu.ignacio.poc.reactivethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.time.Duration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
@DisplayName("IO Controller Test")
class IoControllerTest {

  private static final String IO_URL = "/io";
  private static final String DOWNSTREAM_BODY = "downstream-data";
  private static final String EXPECTED_BODY = "OK:Reactive:IO:" + DOWNSTREAM_BODY;

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @Autowired
  private WebTestClient webTestClient;

  @DynamicPropertySource
  static void configureDownstreamUrl(final DynamicPropertyRegistry registry) {
    registry.add("downstream.service.url", wireMock::baseUrl);
  }

  @Test
  @DisplayName("Should return OK with downstream data via non-blocking WebClient")
  void shouldReturnOkWithDownstreamData() {
    // Given
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
        .withBody(DOWNSTREAM_BODY)));

    // When, Then
    this.webTestClient.get().uri(IO_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String.class).value(_ -> containsString(EXPECTED_BODY));
  }

  @Test
  @DisplayName("Should complete reactively without blocking event loop")
  void shouldCompleteReactivelyWithoutBlocking() {
    // Given
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(DOWNSTREAM_BODY)));

    // When, Then
    this.webTestClient.get().uri(IO_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.contains(EXPECTED_BODY))
      .expectComplete()
      .verify(Duration.ofSeconds(3));
  }

  @Test
  @DisplayName("Should include downstream response body in result")
  void shouldIncludeDownstreamBodyInResult() {
    // Given
    final var customBody = "custom-response-99";
    wireMock.stubFor(WireMock.get(urlEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(customBody)));

    // When
    final var result = this.webTestClient.get().uri(IO_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult();

    // Then
    assertThat(result.getResponseBody()).contains(customBody);
    wireMock.verify(1, WireMock.getRequestedFor(urlEqualTo("/api/data")));
  }
}
