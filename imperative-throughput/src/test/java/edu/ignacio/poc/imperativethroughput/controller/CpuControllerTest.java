package edu.ignacio.poc.imperativethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import edu.ignacio.poc.imperativethroughput.config.MetricsConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@WebMvcTest({CpuController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@Import({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class, MetricsConfiguration.class})
@TestPropertySource(properties = "downstream.service.url=http://localhost:9090")
@DisplayName("CPU Controller Test")
class CpuControllerTest {

  private static final String CPU_URL = "/cpu";
  private static final String EXPECTED_BODY = "OK:Imperative:CPU:fib(50)=";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CpuController controller;

  @Test
  @DisplayName("Should return OK with fibonacci result")
  void shouldReturnOkWithFibonacciResult() throws Exception {
    // When
    final var mvcResult = this.mockMvc.perform(get(CPU_URL))
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
    // When
    final var future = this.controller.getCpu();

    // Then - future must not be done immediately (work runs on virtual thread)
    assertThat(future).isNotNull();

    final var response = future.get(3, TimeUnit.SECONDS);
    assertThat(response)
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().contains(EXPECTED_BODY));
  }

  @Test
  @DisplayName("Should return correct fibonacci value for n=50")
  void shouldReturnCorrectFibonacciValue() throws Exception {
    // Given
    final var expected = "12586269025"; // fib(50) = 12586269025

    // When
    final var response = this.controller.getCpu().get(3, TimeUnit.SECONDS);

    // Then
    assertThat(response.getBody()).contains(expected);
  }
}
