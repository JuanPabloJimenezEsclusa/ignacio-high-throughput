---
layout: default
title: agents
---

# agents

## Quick Reference

| Task | Command |
|---|---|
| Build (all, Maven) | `mvn clean install` |
| Build (all, Gradle) | `gradle clean build` |
| Build (skip tests) | `mvn clean install -DskipTests` |
| Run all tests | `mvn test` |
| Single test class | `mvn test -Dtest=ClassName -f <module>/pom.xml` |
| Run imperative (port 8888) | `mvn spring-boot:run -f imperative-throughput/pom.xml` |
| Run reactive (port 9999) | `mvn spring-boot:run -f reactive-throughput/pom.xml` |
| Format / lint | `mvn rewrite:runNoFork -Popen-rewrite` |

## Modules

| Module | Port | Description |
|---|---|---|
| `imperative-throughput` | 8888 | Spring MVC, virtual threads, blocking I/O |
| `reactive-throughput` | 9999 | Spring WebFlux, Reactor, non-blocking |
| `coverage-jacoco` | — | Aggregated JaCoCo coverage reports |
| `deploy-orchestrator` | — | Docker Compose, K8s Kind, AWS ECS |

## Key Conventions

- **Tech:** Java 25, Spring Boot 4.0+, Maven 3.9+, Gradle 9.4+
- **Formatting:** 2-space indent, 100-char lines, LF, UTF-8 (`.editorconfig`)
- **Imports:** 3 sections alphabetically, no wildcards — `java.*`/`javax.*` → third-party → `edu.ignacio.poc.*`
- **Types:** `final var`, `final` params; `CompletableFuture` (imperative), `Mono`/`Flux` (reactive); avoid `Optional` and Stream API
- **Tests:** `should` prefix, `@DisplayName`, Given/When/Then, AssertJ assertions
- **Naming:** PascalCase classes, camelCase methods/vars, UPPER_SNAKE_CASE constants, `log` for logger
- **Virtual threads:** `Thread.ofVirtual()`, `Executors.newVirtualThreadPerTaskExecutor()`
- **Error handling:** `@ControllerAdvice` (imperative), `AbstractErrorWebExceptionHandler` (reactive)
- **Logging:** `LoggerFactory.getLogger(ClassName.class)`, always include throwable in error logs
- **License:** GPL-3.0-only — include `LICENSE.txt` in `META-INF/`

## Testing Infrastructure

**Location:** `testing/` — performance tests with K6, JMeter, Gatling, Bruno

## Detail Files

| File | Contents |
|---|---|
| [`.ai/build.md`](.ai/build.md) | Full build & run commands |
| [`.ai/testing.md`](.ai/testing.md) | Test commands, coverage, patterns |
| [`.ai/code-style.md`](.ai/code-style.md) | Formatting, naming, types, errors, logging |
| [`.ai/project.md`](.ai/project.md) | Modules, tech stack, tools, notes |
