package edu.ignacio.poc.imperativethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@AutoConfigureMockMvc
@WebMvcTest({SmokeController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@DisplayName("Smoke Controller Test")
class SmokeControllerTest {

  private static final String SMOKES_URL = "/smokes";
  private static final String EXPECTED_BODY = "OK:Imperative:";

  @Autowired
  private MockMvcTester mockMvcTester;

  @Autowired
  private SmokeController controller;

  @Autowired
  private SmokeExceptionHandler exceptionHandler;

  @Test
  @DisplayName("Should return OK with correct cache control headers")
  void shouldReturnOkWithCorrectCacheControlHeaders() {
    // When, Then
    assertThat(mockMvcTester.get().uri(SMOKES_URL))
      .isNotNull()
      .hasStatusOk()
      .hasHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
      .bodyText().contains(EXPECTED_BODY);
  }

  @Test
  @DisplayName("Should take at least 300ms to respond")
  void shouldTakeAtLeast300msToRespond() {
    // Given
    final long startTime = System.currentTimeMillis();

    // When, Then
    assertThat(mockMvcTester.get().uri(SMOKES_URL))
      .isNotNull()
      .hasStatusOk()
      .hasHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
      .bodyText().contains(EXPECTED_BODY);

    final long executionTime = System.currentTimeMillis() - startTime;
    assertTrue(executionTime >= 300,
      "Expected execution time to be at least 300ms but was %dms".formatted(executionTime));
  }

  @Test
  @DisplayName("Should create correct ResponseEntity structure")
  void shouldCreateCorrectResponseEntityStructure() {
    // When, Then
    assertThat(controller.getSmoke())
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().contains(EXPECTED_BODY))
      .returns("no-cache", r -> r.getHeaders().getCacheControl());

  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/smokes",
    "/smokes/"
  })
  @DisplayName("Should handle requests with context path variations")
  void shouldHandleRequestsWithContextPathVariations(final String endpoint) {
    // When, Then
    assertThat(mockMvcTester.get().uri(endpoint))
      .isNotNull()
      .hasStatusOk()
      .hasHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
      .bodyText().contains(EXPECTED_BODY);
  }

  @Test
  @DisplayName("Should throw RuntimeException when sleep is interrupted")
  void shouldThrowRuntimeExceptionWhenSleepIsInterrupted() throws Exception {
    // Given
    final AtomicReference<Throwable> thrownException = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    // When
    final Thread testThread = new Thread(() -> {
      try {
        Thread.currentThread().interrupt();
        controller.getSmoke();
      } catch (Exception e) {
        thrownException.set(e);
      } finally {
        latch.countDown();
      }
    });
    testThread.start();
    boolean await = latch.await(1, TimeUnit.SECONDS);

    // Then
    assertTrue(await);
    assertNotNull(thrownException.get());
    assertInstanceOf(IllegalCallerException.class, thrownException.get());
  }

  @Test
  @DisplayName("Should handle exceptions using ExceptionHandler")
  void shouldHandleExceptionsUsingExceptionHandler() throws Exception {
    // Given
    final var mockMvcWithException = MockMvcBuilders.standaloneSetup(
        new SmokeController() {
          @Override
          public ResponseEntity<String> getSmoke() {
            throw new IllegalCallerException("Interrupted while sleeping");
          }
        })
      .setControllerAdvice(exceptionHandler)
      .build();

    // When, Then
    mockMvcWithException.perform(get(SMOKES_URL))
      .andExpect(status().is5xxServerError())
      .andExpect(content().string("Interrupted while sleeping"));
  }
}
