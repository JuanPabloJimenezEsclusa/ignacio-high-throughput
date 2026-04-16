package edu.ignacio.poc.imperativethroughput.controller;

import java.util.Locale;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Base class for imperative controllers to reduce boilerplate.
 * Handles Timer registration and provides a standard OK response builder.
 */
public abstract class AbstractImperativeController {

  protected final RestClient restClient;
  protected final Timer timer;

  /**
   * Constructor for controllers requiring a RestClient.
   *
   * @param downstreamUrl the downstream service URL
   * @param meterRegistry the meter registry
   * @param endpointName  the endpoint name for tags and description
   */
  protected AbstractImperativeController(
    final String downstreamUrl,
    final MeterRegistry meterRegistry,
    final String endpointName
  ) {
    this.restClient = RestClient.builder()
      .baseUrl(downstreamUrl)
      .build();
    this.timer = Timer.builder("http.request.duration")
      .description(endpointName + " endpoint request duration")
      .tag("module", "imperative")
      .tag("endpoint", endpointName.toLowerCase(Locale.getDefault()))
      .register(meterRegistry);
  }

  /**
   * Constructor for controllers that do NOT require a RestClient.
   *
   * @param meterRegistry the meter registry
   * @param endpointName  the endpoint name for tags and description
   */
  protected AbstractImperativeController(
    final MeterRegistry meterRegistry,
    final String endpointName
  ) {
    this.restClient = null;
    this.timer = Timer.builder("http.request.duration")
      .description(endpointName + " endpoint request duration")
      .tag("module", "imperative")
      .tag("endpoint", endpointName.toLowerCase(Locale.getDefault()))
      .register(meterRegistry);
  }

  /**
   * Returns a ResponseEntity builder with standard headers (no-cache, JSON).
   *
   * @return the response entity builder
   */
  protected ResponseEntity.BodyBuilder okResponse() {
    return ResponseEntity.ok()
      .cacheControl(CacheControl.noCache())
      .contentType(MediaType.APPLICATION_JSON);
  }
}
