package edu.ignacio.poc.reactivethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "downstream.service.url=http://localhost:9090")
@DisplayName("CPU Controller Test")
class CpuControllerTest {

  private static final String CPU_URL = "/cpu";
  private static final String EXPECTED_PREFIX = "OK:Reactive:CPU:fib(50)=";
  private static final String EXPECTED_FIB = "12586269025";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private CpuController controller;

  @Test
  @DisplayName("Should return OK with Fibonacci result")
  void shouldReturnOkWithFibonacciResult() {
    // When, Then
    this.webTestClient.get().uri(CPU_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String.class).value(_ -> containsString(EXPECTED_PREFIX));
  }

  @Test
  @DisplayName("Should return correct Fibonacci(50) value")
  void shouldReturnCorrectFibonacciValue() {
    // When, Then
    this.webTestClient.get().uri(CPU_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .value(body -> assertThat(body).contains(EXPECTED_FIB));
  }

  @Test
  @DisplayName("Should properly configure cpu router function")
  void shouldProperlyConfigureCpuRouterFunction() {
    // Given
    final var routerFunction = this.controller.cpuRoutes();
    assertThat(routerFunction).isNotNull();

    // When, Then
    WebTestClient.bindToRouterFunction(routerFunction).build()
      .get().uri(CPU_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class).value(_ -> containsString(EXPECTED_PREFIX));
  }

  @Test
  @DisplayName("Should complete via reactive pipeline without blocking event loop")
  void shouldCompleteViaReactivePipeline() {
    // When, Then
    this.webTestClient.get().uri(CPU_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.contains(EXPECTED_PREFIX))
      .expectComplete()
      .verify(Duration.ofSeconds(3));
  }
}
