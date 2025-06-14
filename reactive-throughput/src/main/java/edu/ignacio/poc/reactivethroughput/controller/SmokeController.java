package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * The type Smoke controller.
 */
@Configuration
public class SmokeController extends AbstractReactiveController {

  private static final Logger log = LoggerFactory.getLogger(SmokeController.class);

  SmokeController(final MeterRegistry meterRegistry) {
    super(meterRegistry, "smoke");
  }

  /**
   * Shared virtual-thread-backed scheduler — created once, reused across all requests.
   *
   * @return the scheduler
   */
  @Bean
  public Scheduler virtualThreadScheduler() {
    return Schedulers.fromExecutorService(
      Executors.newVirtualThreadPerTaskExecutor(),
      "virtual-thread"
    );
  }

  /**
   * Smokes routes router function.
   *
   * @return the router function
   */
  @Bean
  public RouterFunction<ServerResponse> smokesRoutes(final Scheduler virtualThreadScheduler) {
    return route(
      GET("/smokes").or(GET("/smokes/")),
      request -> this.okResponse()
        .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
        .body(fromPublisher(Mono.just("OK:Reactive:%s".formatted(Thread.currentThread().toString()))
          .delayElement(Duration.ofMillis(300L)), String.class))
        .doOnNext(result -> log.info("Smoke reactive endpoint - status: {} - thread: {}",
          result.statusCode(), Thread.currentThread()))
        .doOnError(throwable -> log.error("Error in smoke endpoint: {}",
          request.uri().getUserInfo(), throwable))
        .subscribeOn(virtualThreadScheduler));
  }
}
