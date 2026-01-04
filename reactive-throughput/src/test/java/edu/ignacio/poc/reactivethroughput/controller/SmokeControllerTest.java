package edu.ignacio.poc.reactivethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Smoke Controller Test")
class SmokeControllerTest {

  private static final String THROUGHPUT_SMOKES = "/smokes";
  private static final String EXPECTED_BODY = "OK:Reactive";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private SmokeController controller;

  @Test
  @DisplayName("Should return OK response with correct headers")
  void shouldReturnOkResponseWithCorrectHeaders() {
    // When
    final var response = webTestClient
      .get()
      .uri(THROUGHPUT_SMOKES)
      .exchange();

    // Then
    response.expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectHeader().valueMatches(HttpHeaders.CACHE_CONTROL, "no-cache")
      .expectBody(String.class).value(_ -> containsString(EXPECTED_BODY));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/smokes",
    "/smokes/",
  })
  @DisplayName("Should handle different path variations")
  void shouldHandlePathVariations(final String path) {
    // When, Then
    webTestClient
      .get()
      .uri(path)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectHeader().valueMatches(HttpHeaders.CACHE_CONTROL, "no-cache")
      .expectBody(String.class).value(_ -> containsString(EXPECTED_BODY));
  }

  @Test
  @DisplayName("Should take at least 300ms to respond")
  void shouldTakeAtLeast300msToRespond() {
    // Given
    final long startTime = System.currentTimeMillis();

    // When
    webTestClient
      .get()
      .uri(THROUGHPUT_SMOKES)
      .exchange()
      .expectStatus().isOk();

    // Then
    final long executionTime = System.currentTimeMillis() - startTime;
    assertTrue(executionTime >= 300,
      "Expected execution time to be at least 300ms but was %dms".formatted(executionTime));
  }

  @Test
  @DisplayName("Should properly configure router function")
  void shouldProperlyConfigureRouterFunction() {
    // Given
    final var routerFunction = controller.smokesRoutes();

    // When
    assertNotNull(routerFunction);

    // Then
    WebTestClient.bindToRouterFunction(routerFunction).build()
      .get().uri(THROUGHPUT_SMOKES)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class).value(_ -> containsString(EXPECTED_BODY));
  }

  @Test
  @DisplayName("Should include delay in response")
  void shouldIncludeDelayInResponse() {
    // When, Then
    webTestClient
      .get()
      .uri(THROUGHPUT_SMOKES)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.contains(EXPECTED_BODY))
      .expectComplete()
      .verify(Duration.ofMillis(400));
  }

  @Test
  @DisplayName("Should set correct cache control headers in router function")
  void shouldSetCorrectCacheControlHeadersInRouterFunction() {
    // Given
    final var routerClient = WebTestClient.bindToRouterFunction(controller.smokesRoutes()).build();

    // When
    final var response = routerClient
      .get()
      .uri(THROUGHPUT_SMOKES)
      .exchange();

    // Then
    final var cacheControlHeader = response.returnResult(String.class)
      .getResponseHeaders()
      .getFirst(HttpHeaders.CACHE_CONTROL);

    assertNotNull(cacheControlHeader);
    assertThat(cacheControlHeader).contains("no-cache");
  }

  @Test
  @DisplayName("Should handle exceptions through GlobalExceptionHandler")
  void shouldHandleExceptionsWithGlobalHandler() {
    // When, Then
    webTestClient.get()
      .uri("/non-existent-path")
      .exchange()
      .expectStatus().is5xxServerError()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$.status").exists()
      .jsonPath("$.error").exists();
  }

  @Test
  @DisplayName("Should handle different types of exceptions")
  void shouldHandleDifferentExceptionTypes() {
    // Given
    final var config = new TestErrorConfig();
    final var testClient = WebTestClient.bindToRouterFunction(config.testErrorRoutes())
      .build();

    // When, Then
    testClient
      .get()
      .uri("/illegal-arg-error")
      .exchange()
      .expectStatus().is5xxServerError()
      .expectBody();

    testClient
      .get()
      .uri("/runtime-error")
      .exchange()
      .expectStatus().is5xxServerError()
      .expectBody();
  }

  @Configuration
  private static class TestErrorConfig {
    @Bean
    public RouterFunction<ServerResponse> testErrorRoutes() {
      return route(GET("/illegal-arg-error"),
        _ -> Mono.error(new IllegalArgumentException("Illegal argument")))
        .andRoute(GET("/runtime-error"),
          _ -> Mono.error(new RuntimeException("Runtime exception")));
    }
  }
}
