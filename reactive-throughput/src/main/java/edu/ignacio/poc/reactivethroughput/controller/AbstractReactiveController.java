package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.Locale;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Base class for reactive controllers to reduce boilerplate.
 * Handles Timer registration and provides a standard OK response builder.
 */
public abstract class AbstractReactiveController {

  protected final WebClient webClient;
  protected final Timer timer;

  /**
   * Constructor for controllers requiring a WebClient.
   *
   * @param downstreamUrl the downstream service URL
   * @param meterRegistry the meter registry
   * @param endpointName  the endpoint name for tags and description
   */
  protected AbstractReactiveController(
    @NonNull final String downstreamUrl,
    @NonNull final MeterRegistry meterRegistry,
    @NonNull final String endpointName
  ) {
    this.webClient = WebClient.builder()
      .baseUrl(downstreamUrl)
      .build();
    this.timer = Timer.builder("http.request.duration")
      .description(endpointName + " endpoint request duration")
      .tag("module", "reactive")
      .tag("endpoint", endpointName.toLowerCase(Locale.getDefault()))
      .register(meterRegistry);
  }

  /**
   * Constructor for controllers that do NOT require a WebClient.
   *
   * @param meterRegistry the meter registry
   * @param endpointName  the endpoint name for tags and description
   */
  protected AbstractReactiveController(
    @NonNull final MeterRegistry meterRegistry,
    @NonNull final String endpointName
  ) {
    this.webClient = null;
    this.timer = Timer.builder("http.request.duration")
      .description(endpointName + " endpoint request duration")
      .tag("module", "reactive")
      .tag("endpoint", endpointName.toLowerCase(Locale.getDefault()))
      .register(meterRegistry);
  }

  /**
   * Returns a ServerResponse.BodyBuilder with standard headers (no-cache, JSON).
   *
   * @return the body builder
   */
  protected ServerResponse.BodyBuilder okResponse() {
    return ok()
      .cacheControl(CacheControl.noCache())
      .contentType(MediaType.APPLICATION_JSON);
  }
}
