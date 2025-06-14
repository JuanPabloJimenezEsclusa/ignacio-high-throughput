package edu.ignacio.poc.imperativethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SmokeController.class)
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@DisplayName("Smoke Controller Test")
class SmokeControllerTest {

  private static final String SMOKES_URL = "/smokes";
  private static final String EXPECTED_BODY = "OK:Imperative:";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SmokeController controller;

  @Test
  @DisplayName("Should return OK with correct cache control headers")
  void shouldReturnOkWithCorrectCacheControlHeaders() throws Exception {
    // When, Then
    mockMvc.perform(get(SMOKES_URL))
      .andExpectAll(
        status().isOk(),
        content().string(containsString(EXPECTED_BODY)),
        header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")));
  }

  @Test
  @DisplayName("Should take at least 300ms to respond")
  void shouldTakeAtLeast300msToRespond() throws Exception {
    // Given
    final long startTime = System.currentTimeMillis();

    // When, Then
    mockMvc.perform(get(SMOKES_URL))
      .andExpectAll(
        status().isOk(),
        content().string(containsString(EXPECTED_BODY)),
        header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")));

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
  void shouldHandleRequestsWithContextPathVariations(final String endpoint) throws Exception {
    // When, Then
    mockMvc.perform(get(endpoint))
      .andExpectAll(
        status().isOk(),
        content().string(containsString(EXPECTED_BODY)),
        header().string(HttpHeaders.CACHE_CONTROL, containsString("no-cache")));
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
}
