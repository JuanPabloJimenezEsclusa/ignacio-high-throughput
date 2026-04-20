# Agent Development Guide

Context split into focused files under `.ai/`:

| File                                     | Contents                                           |
|------------------------------------------|----------------------------------------------------|
| [`.ai/project.md`](.ai/project.md)       | Modules, tech stack, misc notes                    |
| [`.ai/build.md`](.ai/build.md)           | Build & run commands (Maven + Gradle)              |
| [`.ai/testing.md`](.ai/testing.md)       | Test commands, coverage, test patterns             |
| [`.ai/code-style.md`](.ai/code-style.md) | Formatting, naming, types, error handling, logging |

## Key Modules

- **`imperative-throughput`** — Spring MVC with virtual threads (port 8888)
- **`reactive-throughput`** — Spring WebFlux with Reactor (port 9999)
- **`coverage-jacoco`** — Aggregated JaCoCo test coverage
- **`deploy-orchestrator`** — Deployment configs for Docker Compose, K8s Kind, AWS ECS

## Testing Infrastructure

- **Location:** `testing/` directory
- **Tools:** K6, JMeter, Gatling, Bruno (HTTP client), HTTP request files
- **API endpoints:** See [Readme.adoc](./Readme.adoc) sections on API Endpoints and Comparison

## Deployment Options

- **Docker Compose** — `deploy-orchestrator/docker-compose/`
- **Kubernetes (Kind)** — `deploy-orchestrator/k8s-kind/`
- **AWS ECS** — `deploy-orchestrator/aws-ecs/`

