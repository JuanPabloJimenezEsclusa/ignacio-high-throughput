---
layout: default
title: testing (jmeter)
---

# testing (jmeter)

> • [Dependencies](#dependencies)
  • [Architecture](#architecture)
  • [Running the tests](#running-the-tests)

This module holds the performance tests for the Spring Boot application, using [JMeter](https://jmeter.apache.org/).

## Dependencies

* [JDK ~25](https://openjdk.org/projects/jdk/25/)
* [JMeter ~5.6](https://jmeter.apache.org/changes.html)

## Architecture

| File | Description |
|------|-------------|
| [high-throughput-performance.jmx](./high-throughput-performance.jmx) | Test plan. It calls `GET /smokes` on both modules |
| [run.sh](./run.sh) | Runs the test |
| [reports/aws/run-1/html/index.html](./reports) | HTML summary of one run |

## Running the tests

```bash
# Start the application with Docker Compose
./../../deploy-orchestrator/docker-compose/start.sh
```
```bash
# Run the test
./run.sh
```
```bash
# Stop the application
./../../deploy-orchestrator/docker-compose/stop.sh
```

The runner accepts `ENVIRONMENT`, `RUNS`, `THREADS` (default `7000`), `RAMP_UP` (default `20` seconds) and `LOOPS` (default `20`). See [testing conventions](../Readme.md).
