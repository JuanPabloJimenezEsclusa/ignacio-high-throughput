package edu.ignacio.poc.imperativethroughput.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Smoke controller.
 */
@RestController
public class SmokeController  {

  private static final Logger log = LoggerFactory.getLogger(SmokeController.class);

  /**
   * Gets smoke.
   *
   * @return the smoke
   */
  @GetMapping({"/smokes", "/smokes/"})
  public ResponseEntity<String> getSmoke() {
    try {
      Thread.sleep(300);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      throw new IllegalCallerException("Interrupted while sleeping");
    }

    log.info("Smoke imperative endpoint");
    return ResponseEntity.ok()
      .cacheControl(CacheControl.noCache())
      .contentType(MediaType.APPLICATION_JSON)
      .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
      .body("OK:Imperative:%s".formatted(Thread.currentThread().toString()));
  }
}
