package edu.ignacio.poc.reactivethroughput;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "downstream.service.url=http://localhost:9090")
class ReactiveThroughputApplicationTests {
  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    Assertions.assertNotNull(this.applicationContext);
  }

  @Test
  void mainMethodStartsApplication() {
    ReactiveThroughputApplication.main(new String[]{});
    Assertions.assertTrue(true);
  }
}
