package edu.ignacio.poc.imperativethroughput;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ImperativeThroughputApplicationTests {
  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    Assertions.assertNotNull(this.applicationContext);
  }

  @Test
  void mainMethodStartsApplication() {
    ImperativeThroughputApplication.main(new String[]{});
    Assertions.assertTrue(true);
  }
}
