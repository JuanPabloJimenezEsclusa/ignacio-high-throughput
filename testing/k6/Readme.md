---
layout: default
title: testing (k6)
---

# testing (k6)

> • [Dependencies](#dependencies)
  • [Architecture](#architecture)
  • [Running the tests](#running-the-tests)

This module holds the performance tests for the Spring Boot application, using [k6](https://k6.io/).

## Dependencies

* [k6](https://hub.docker.com/r/grafana/k6), image `grafana/k6:master-with-browser`
* [Docker ~29](https://docs.docker.com/engine/release-notes/29/)

## Architecture

| File | Description |
|------|-------------|
| [high-throughput-load-tests.js](./script/high-throughput-load-tests.js) | Test plan. Each iteration calls 13 endpoints, 6 on the imperative module and 7 on the reactive one |
| [run.sh](./run.sh) | Runs the test |
| [result/aws/run-1/summary.html](./result) | HTML summary of one run |

## Running the tests

```bash
# Start the application with Docker Compose
./../../deploy-orchestrator/docker-compose/start.sh
```
```bash
# Run the test. The live dashboard is at http://localhost:5665/ui/
./run.sh
```
```bash
# Stop the application
./../../deploy-orchestrator/docker-compose/stop.sh
```

The runner accepts `ENVIRONMENT` and `RUNS`. See [testing conventions](../Readme.md).
