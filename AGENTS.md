# Agent Development Guide

This document provides coding agents with essential information about building, testing, and maintaining code in the ignacio-high-throughput project.

## Project Overview

Multi-module Spring Boot project comparing imperative vs. reactive approaches for high-throughput Java applications.

**Modules:**
- `imperative-throughput` - Traditional Spring MVC with blocking operations and virtual threads
- `reactive-throughput` - Spring WebFlux with non-blocking reactive streams
- `coverage-jacoco` - Aggregated test coverage reports
- `testing/` - Performance testing tools (K6, JMeter, Gatling)

**Tech Stack:** Java 25, Spring Boot 4.0.2, Maven 3.9+, Gradle 9.1+, Docker

## Build Commands

### Maven

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -f imperative-throughput/pom.xml
mvn clean install -f reactive-throughput/pom.xml

# Build without tests
mvn clean install -DskipTests

# Format code with OpenRewrite
mvn rewrite:runNoFork -Popen-rewrite

# Generate changelog
mvn generate-sources
```

### Gradle

```bash
# Build all modules
gradle clean build

# Build specific module
gradle :imperative-throughput:build
gradle :reactive-throughput:build

# Build without tests
gradle build -x test
```

## Test Commands

### Run All Tests

```bash
# Maven
mvn test

# Gradle
gradle test
```

### Run Single Test (Maven)

```bash
# Run single test class
mvn test -Dtest=SmokeControllerTest -f imperative-throughput/pom.xml

# Run single test method
mvn test -Dtest=SmokeControllerTest#shouldReturnOkAfterDelay -f imperative-throughput/pom.xml

# Run tests matching pattern
mvn test -Dtest=*ControllerTest -f reactive-throughput/pom.xml
```

### Run Single Test (Gradle)

```bash
# Run single test class
gradle :imperative-throughput:test --tests SmokeControllerTest

# Run single test method
gradle :imperative-throughput:test --tests SmokeControllerTest.shouldReturnOkAfterDelay

# Run tests matching pattern
gradle :reactive-throughput:test --tests *ControllerTest
```

### Coverage Reports

```bash
# Maven - generates reports in target/site/jacoco/
mvn test jacoco:report

# Gradle - generates reports in build/reports/jacoco/
gradle test jacocoTestReport
```

## Running Applications

### Imperative Module

```bash
# Maven
mvn spring-boot:run -f imperative-throughput/pom.xml

# Gradle
gradle :imperative-throughput:bootRun

# JAR
java -jar imperative-throughput/target/imperative-throughput-1.0.0.jar

# Access at: http://localhost:8888/imperative-throughput/
```

### Reactive Module

```bash
# Maven
mvn spring-boot:run -f reactive-throughput/pom.xml

# Gradle
gradle :reactive-throughput:bootRun

# JAR
java -jar reactive-throughput/target/reactive-throughput-1.0.0.jar

# Access at: http://localhost:9999/reactive-throughput/
```

## Code Style Guidelines

### Formatting Rules (.editorconfig)

- **Indentation:** 2 spaces (NO tabs)
- **Line length:** 100 characters maximum
- **End of line:** LF (Unix style)
- **Charset:** UTF-8
- **Final newline:** Not required

### Import Organization

Organize imports in three sections (alphabetically sorted within each):

1. Java standard library (`java.*`, `javax.*`)
2. Third-party libraries (Spring, JUnit, etc.)
3. Project imports (`edu.ignacio.poc.*`)

**NO wildcard imports** - import specific classes only.

### Naming Conventions

- **Classes:** PascalCase (`SmokeController`, `GlobalExceptionHandler`)
- **Methods:** camelCase (`getSmoke()`, `formatErrorResponse()`)
- **Variables:** camelCase with descriptive names (`errorAttributes`, `startTime`)
- **Constants:** UPPER_SNAKE_CASE (`SMOKES_URL`, `EXPECTED_BODY`)
- **Logger:** Static final field named `log` (lowercase)
- **Test methods:** Descriptive with "should" prefix (`shouldReturnOkWithCorrectHeaders()`)

### Type Usage

**Always use:**
- `final var` for local variable type inference
- `final` modifier on method parameters
- `@NonNull` annotations (reactive module) for null-safety

**Commonly used types:**
- `CompletableFuture` (imperative async)
- `Mono`/`Flux` (reactive async)
- `Duration` for time periods
- `List.of()`, `Map.of()` for immutable collections

**Avoid:**
- `Optional` (not commonly used in this codebase)
- Stream API (prefer reactive or direct loops)
- Wildcard imports

### Class Structure

1. Package declaration
2. Imports (organized in 3 sections)
3. Javadoc class comment ("The type [ClassName]")
4. Class annotations (`@RestController`, `@Configuration`, etc.)
5. Class declaration
6. Static fields (logger, executors, constants)
7. Instance fields
8. Constructor
9. Public methods
10. Private methods

### Error Handling

**Imperative module:**
```java
try {
  // operation
} catch (Exception e) {
  log.error("Error message", e);
  future.completeExceptionally(e);
}
```

**Reactive module:**
```java
.doOnError(throwable -> 
  log.error("Error in endpoint: {}", request.uri(), throwable))
```

**Global exception handling:**
- Use `@ControllerAdvice` with `@ExceptionHandler` (imperative)
- Extend `AbstractErrorWebExceptionHandler` (reactive)
- Return appropriate HTTP status codes (5xx for server errors)

### Logging

```java
private static final Logger log = LoggerFactory.getLogger(ClassName.class);

// Include context in logs
log.info("Processing request on thread: {}", Thread.currentThread());
log.error("Error message: {}", details, throwable);
```

### Testing Patterns

**Test structure:**
```java
@DisplayName("Should do something specific")
@Test
void shouldDoSomethingSpecific() {
  // Given
  final var input = prepareInput();
  
  // When
  final var result = performOperation(input);
  
  // Then
  assertThat(result)
    .isNotNull()
    .returns(expectedValue, ClassName::getValue);
}
```

**Annotations:**
- `@DisplayName` on all test classes and methods
- `@SpringBootTest` for integration tests
- `@WebMvcTest` for imperative controller tests
- `@ParameterizedTest` with `@ValueSource` for multiple inputs

**Assertions:**
- Use **AssertJ** fluent assertions: `assertThat(value).isNotNull().isEqualTo(expected)`
- Use **method references** in returns: `.returns(expected, ClassName::getField)`
- Use **lambda predicates** for complex checks: `body -> body.contains("expected")`

**Test clients:**
- `MockMvc` for imperative module
- `WebTestClient` for reactive module
- `StepVerifier` for reactive stream testing

## Git Commit Messages

Follow conventional commits style:

- Use present tense: "Add feature" not "Added feature"
- Use imperative mood: "Move cursor to..." not "Moves cursor to..."
- Limit first line to 72 characters
- Reference issues: `#123`

**Examples:**
```
Add smoke endpoint with virtual thread support

Fix error handling in reactive module

Update dependencies to Spring Boot 4.0.2
```

## Additional Notes

- **Virtual threads:** Extensively used in imperative module (`Thread.ofVirtual()`, `Executors.newVirtualThreadPerTaskExecutor()`)
- **PMD suppressions:** Use `@SuppressWarnings("PMD.RuleName")` sparingly when justified
- **OpenRewrite:** Configured for IntelliJ and Spring formatting styles
- **Documentation:** Add Javadoc to all public classes and methods
- **License:** GPL-3.0-only - include LICENSE.txt in META-INF/
