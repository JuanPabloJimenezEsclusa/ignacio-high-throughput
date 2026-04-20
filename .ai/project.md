# Project Overview

Multi-module Spring Boot project comparing imperative vs. reactive approaches for high-throughput Java applications.

## Modules

| Module | Description |
|---|---|
| `imperative-throughput` | Traditional Spring MVC, blocking ops, virtual threads |
| `reactive-throughput` | Spring WebFlux, non-blocking reactive streams |
| `coverage-jacoco` | Aggregated test coverage reports |
| `deploy-orchestrator` | Deployment configurations (Docker Compose, K8s Kind, AWS ECS) |

## Tools & Resources

| Directory | Contents |
|---|---|
| `testing/` | Performance testing tools (K6, JMeter, Gatling, Bruno HTTP client) |
| `resource-profiling/` | JFR profiling configurations and custom.jfc settings |

## Tech Stack

- **Java 25**, Spring Boot 4.0+
- **Build:** Maven 3.9+, Gradle 9.4+
- **Infra:** Docker

## Notes

- **Virtual threads:** Extensively used in imperative module (`Thread.ofVirtual()`, `Executors.newVirtualThreadPerTaskExecutor()`)
- **PMD suppressions:** Use `@SuppressWarnings("PMD.RuleName")` sparingly when justified
- **OpenRewrite:** Configured for IntelliJ and Spring formatting styles
- **Documentation:** Add Javadoc to all public classes and methods
- **License:** GPL-3.0-only - include `LICENSE.txt` in `META-INF/`
