package dev.jpje.reactivethroughput.controller;

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

  protected ServerResponse.BodyBuilder okResponse() {
    return ok()
      .cacheControl(CacheControl.noCache())
      .contentType(MediaType.APPLICATION_JSON);
  }
}
