package edu.ignacio.poc.imperativethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@AutoConfigureMockMvc
@WebMvcTest({SmokeController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = {ImperativeThroughputApplication.class, SmokeControllerTest.TestConfig.class})
@DisplayName("Smoke Controller Test")
class SmokeControllerTest {

  @Configuration
  static class TestConfig {
    @Bean
    public MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  private static final String SMOKES_URL = "/smokes";
  private static final String EXPECTED_BODY = "OK:Imperative:";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SmokeController controller;

  @Autowired
  private SmokeExceptionHandler exceptionHandler;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  @DisplayName("Should return OK with correct cache control headers")
  void shouldReturnOkWithCorrectCacheControlHeaders() throws Exception {
    // When
    final var mvcResult = this.mockMvc.perform(get(SMOKES_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString(EXPECTED_BODY)));
  }

  @Test
  @DisplayName("Should take at least 300ms to respond")
  void shouldTakeAtLeast300msToRespond() throws Exception {
    // Given
    final long startTime = System.currentTimeMillis();

    // When
    final var mvcResult = this.mockMvc.perform(get(SMOKES_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk());

    // Then
    final long executionTime = System.currentTimeMillis() - startTime;
    assertThat(executionTime).isGreaterThanOrEqualTo(300);
  }

  @Test
  @DisplayName("Should create correct ResponseEntity structure")
  void shouldCreateCorrectResponseEntityStructure() throws Exception {
    // When
    final var futureResponse = this.controller.getSmoke();

    // Then
    assertThat(futureResponse.get(1, TimeUnit.SECONDS))
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
    // When
    final var mvcResult = this.mockMvc.perform(get(endpoint))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString(EXPECTED_BODY)));
  }

  @Test
  @DisplayName("Should handle exceptions in async processing")
  void shouldHandleExceptionsInAsyncProcessing() throws Exception {
    // Given
    final var mockMvcWithException = MockMvcBuilders.standaloneSetup(
        new SmokeController(this.meterRegistry) {
          @Override
          public CompletableFuture<ResponseEntity<String>> getSmoke() {
            final var future = new CompletableFuture<ResponseEntity<String>>();
            future.completeExceptionally(new IllegalCallerException("Interrupted while sleeping"));
            return future;
          }
        })
      .setControllerAdvice(this.exceptionHandler)
      .build();

    // When
    final var mvcResult = mockMvcWithException.perform(get(SMOKES_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    mockMvcWithException.perform(asyncDispatch(mvcResult))
      .andExpect(status().is5xxServerError())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":500")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("\"error\":\"Internal Server Error\"")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("Interrupted while sleeping")));
  }

  @Test
  @DisplayName("Should complete future without blocking thread")
  void shouldCompleteFutureWithoutBlockingThread() throws Exception {
    // Given
    final long startTime = System.currentTimeMillis();

    // When
    final var future = this.controller.getSmoke();

    // Then
    assertThat(future).isNotNull();
    assertThat(future.isDone()).isFalse();

    // Wait for completion
    final var response = future.get(1, TimeUnit.SECONDS);
    final long executionTime = System.currentTimeMillis() - startTime;

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(executionTime).isGreaterThanOrEqualTo(300);
  }
}
