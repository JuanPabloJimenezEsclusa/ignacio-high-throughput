# Testing

## Run Tests

```bash
mvn test                                                              # Maven all
gradle test                                                           # Gradle all

mvn test -Dtest=SmokeControllerTest -f imperative-throughput/pom.xml  # single class
mvn test -Dtest=SmokeControllerTest#shouldReturnOkAfterDelay -f imperative-throughput/pom.xml  # single method
mvn test -Dtest=*ControllerTest -f reactive-throughput/pom.xml        # pattern

gradle :imperative-throughput:test --tests SmokeControllerTest
gradle :imperative-throughput:test --tests SmokeControllerTest.shouldReturnOkAfterDelay
gradle :reactive-throughput:test --tests *ControllerTest
```

## Coverage

```bash
mvn test jacoco:report       # → target/site/jacoco/
gradle test jacocoTestReport # → build/reports/jacoco/
```

## Test Patterns

**Structure:** Given / When / Then with `@DisplayName` and `should` prefix.

```java
@DisplayName("Should do something specific")
@Test
void shouldDoSomethingSpecific() {
  final var input = prepareInput();
  final var result = performOperation(input);
  assertThat(result)
    .isNotNull()
    .returns(expectedValue, ClassName::getValue);
}
```

**Annotations:** `@SpringBootTest` (integration), `@WebMvcTest` (imperative controllers), `@ParameterizedTest` + `@ValueSource`.

**Assertions (AssertJ):**
```java
assertThat(value).isNotNull().isEqualTo(expected)
.returns(expected, ClassName::getField)
```

**Clients:**
- `MockMvc` — imperative
- `WebTestClient` — reactive
- `StepVerifier` — reactive streams
