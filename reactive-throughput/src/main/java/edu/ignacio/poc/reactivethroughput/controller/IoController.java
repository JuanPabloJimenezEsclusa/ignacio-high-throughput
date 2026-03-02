package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * The type Io controller.
 *
 * <p>Demonstrates I/O-bound work using a non-blocking {@link WebClient} call.
 * The event loop thread is never blocked; the request pipeline is suspended
 * and resumed via callbacks when the downstream response arrives. Compare
 * with the imperative counterpart using {@code RestClient} on virtual threads.
 */
@Configuration
public class IoController {

  private static final Logger log = LoggerFactory.getLogger(IoController.class);
  private static final String DOWNSTREAM_PATH = "/api/data";

  private final WebClient webClient;
  private final Timer ioTimer;

  IoController(
    @Value("${downstream.service.url}") @NonNull final String downstreamUrl,
    @NonNull final MeterRegistry meterRegistry
  ) {
    this.webClient = WebClient.builder()
      .baseUrl(downstreamUrl)
      .build();
    this.ioTimer = Timer.builder("http.request.duration")
      .description("IO endpoint request duration")
      .tag("module", "reactive")
      .tag("endpoint", "io")
      .register(meterRegistry);
  }

  /**
   * Io routes router function.
   *
   * @return the router function
   */
  @Bean
  public RouterFunction<ServerResponse> ioRoutes() {
    return route(
      GET("/io").or(GET("/io/")),
      _ -> {
        final var sample = Timer.start();
        return this.webClient.get()
          .uri(DOWNSTREAM_PATH)
          .retrieve()
          .bodyToMono(String.class)
          .flatMap(body -> {
            final var currentThread = Thread.currentThread();
            log.info("IO reactive endpoint - downstream: {} - thread: {}", body, currentThread);
            sample.stop(this.ioTimer);
            return ok()
              .cacheControl(CacheControl.noCache())
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue("OK:Reactive:IO:%s:%s".formatted(body, currentThread));
          })
          .doOnError(throwable -> log.error("Error in IO reactive endpoint", throwable));
      });
  }
}
