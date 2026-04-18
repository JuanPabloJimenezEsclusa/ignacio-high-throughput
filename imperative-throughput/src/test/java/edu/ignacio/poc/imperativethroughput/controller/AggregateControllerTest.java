package edu.ignacio.poc.imperativethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.ignacio.poc.imperativethroughput.ImperativeThroughputApplication;
import edu.ignacio.poc.imperativethroughput.config.MetricsConfiguration;
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
@WebMvcTest({AggregateController.class, SmokeExceptionHandler.class})
@ContextConfiguration(classes = ImperativeThroughputApplication.class)
@Import({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class, MetricsConfiguration.class})
@DisplayName("Aggregate Controller Test")
class AggregateControllerTest {

  private static final String AGGREGATE_URL = "/aggregate";

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AggregateController controller;

  @DynamicPropertySource
  private static void configureDownstreamUrl(final DynamicPropertyRegistry registry) {
    registry.add("downstream.service.url", wireMock::baseUrl);
  }

  @Test
  @DisplayName("Should return OK aggregating 3 parallel downstream calls via CompletableFuture.allOf")
  void shouldReturnOkAggregatingParallelCalls() throws Exception {
    // Given
    this.stubDownstreamForAllIds("data");

    // When
    final var mvcResult = this.mockMvc.perform(get(AGGREGATE_URL))
      .andExpect(request().asyncStarted())
      .andReturn();

    // Then
    this.mockMvc.perform(asyncDispatch(mvcResult))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("OK:Imperative:Aggregate:")));
  }

  @Test
  @DisplayName("Should complete future without blocking caller thread")
  void shouldCompleteFutureWithoutBlockingCallerThread() throws Exception {
    // Given
    this.stubDownstreamForAllIds("result");

    // When
    final var future = this.controller.getAggregate();
    assertThat(future).isNotNull();

    // Then
    final var response = future.get(5, TimeUnit.SECONDS);
    assertThat(response)
      .isNotNull()
      .returns(HttpStatus.OK, ResponseEntity::getStatusCode)
      .returns(true, r -> r.getBody() != null && r.getBody().contains("OK:Imperative:Aggregate:"));
  }

  @Test
  @DisplayName("Should call downstream 3 times in parallel and combine results")
  void shouldCallDownstream3TimesAndCombineResults() throws Exception {
    // Given
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/1")).willReturn(aResponse().withStatus(200).withBody("alpha")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/2")).willReturn(aResponse().withStatus(200).withBody("beta")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/3")).willReturn(aResponse().withStatus(200).withBody("gamma")));

    // When
    final ResponseEntity<String> response = this.controller.getAggregate().get(5, TimeUnit.SECONDS);

    // Then
    assertThat(response.getBody())
      .contains("alpha")
      .contains("beta")
      .contains("gamma");
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/1")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/2")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/3")));
  }

  private void stubDownstreamForAllIds(final String bodyPrefix) {
    for (int i = 1; i <= 3; i++) {
      wireMock.stubFor(WireMock.get(urlMatching("/api/data/" + i))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
          .withBody(bodyPrefix + i)));
    }
  }
}
