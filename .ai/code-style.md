# Code Style

## Formatting (`.editorconfig`)

- Indent: **2 spaces** (no tabs)
- Line length: **100 chars**
- EOL: **LF**, charset: **UTF-8**

## Imports

Three sections, alphabetically sorted, no wildcards:
1. `java.*`, `javax.*`
2. Third-party (Spring, JUnit, etc.)
3. `edu.ignacio.poc.*`

## Naming

| Element | Convention | Example |
|---|---|---|
| Class | PascalCase | `SmokeController` |
| Method/Variable | camelCase | `getSmoke()`, `startTime` |
| Constant | UPPER_SNAKE_CASE | `SMOKES_URL` |
| Logger | lowercase `log` | `log.info(...)` |
| Test method | `should` prefix | `shouldReturnOkWithCorrectHeaders()` |

## Types

**Use:** `final var`, `final` params, `@NonNull` (reactive), `CompletableFuture` (imperative), `Mono`/`Flux` (reactive), `List.of()`, `Map.of()`

**Avoid:** `Optional`, Stream API, wildcard imports

## Class Structure

1. Package → Imports → Javadoc → Annotations → Class declaration
2. Static fields (logger, executors, constants)
3. Instance fields → Constructor → Public methods → Private methods

## Error Handling

```java
// Imperative
try { ... } catch (Exception e) {
  log.error("Error message", e);
  future.completeExceptionally(e);
}

// Reactive
.doOnError(t -> log.error("Error in endpoint: {}", request.uri(), t))
```

- Imperative: `@ControllerAdvice` + `@ExceptionHandler`
- Reactive: extend `AbstractErrorWebExceptionHandler`
- Always return appropriate 5xx codes

## Logging

```java
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
log.info("Processing request on thread: {}", Thread.currentThread());
log.error("Error: {}", details, throwable);
```
