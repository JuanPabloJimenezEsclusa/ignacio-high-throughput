package edu.ignacio.poc.imperativethroughput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The type Imperative throughput application.
 */
@SpringBootApplication
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ImperativeThroughputApplication {

  private ImperativeThroughputApplication() {
  }

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(ImperativeThroughputApplication.class, args);
  }
}
