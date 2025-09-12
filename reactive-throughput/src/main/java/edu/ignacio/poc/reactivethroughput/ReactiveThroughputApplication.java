package edu.ignacio.poc.reactivethroughput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The type Reactive throughput application.
 */
@SpringBootApplication
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ReactiveThroughputApplication {

  private ReactiveThroughputApplication() {
  }

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(ReactiveThroughputApplication.class, args);
  }
}
