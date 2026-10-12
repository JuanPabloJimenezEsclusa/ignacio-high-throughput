---
layout: default
title: testing (gatling)
---

# testing (gatling)

> • [Dependencies](#dependencies)
  • [Architecture](#architecture)
  • [Running the tests](#running-the-tests)
  • [Exploring plugin options](#exploring-plugin-options)

This module holds the performance tests for the Spring Boot application, using [Gatling](https://gatling.io/).

## Dependencies

* [JDK ~25](https://openjdk.org/projects/jdk/25/)
* [Gatling ~3.15](https://docs.gatling.io/tutorials/scripting-intro/)
* [Docker ~29](https://docs.docker.com/engine/release-notes/29/)

## Architecture

| File | Description |
|------|-------------|
| [GatlingHighThroughputSimulation.java](./src/main/java/dev/jpje/testing/gatling/GatlingHighThroughputSimulation.java) | Test plan. It calls every comparable endpoint on both modules |
| [run.sh](./run.sh) | Runs the simulation |
| [index.html](./target/gatling/gatlinghighthroughputsimulation-latest/index.html) | HTML report of the last run |

## Running the tests

```bash
# Start the application with Docker Compose
./../../deploy-orchestrator/docker-compose/start.sh
```
```bash
# Run the simulation
./run.sh
```
```bash
# Stop the application
./../../deploy-orchestrator/docker-compose/stop.sh
```

The runner accepts `ENVIRONMENT` and `RUNS`. See [testing conventions](../Readme.md).

## Exploring plugin options

```bash
mvn gatling:help -Ddetail=true -Dgoal=test
```
