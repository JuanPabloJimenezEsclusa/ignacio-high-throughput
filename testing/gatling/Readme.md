# Gatling tests

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Running the Tests](#-running-the-tests)
  • [Exploring plugin options](#-exploring-plugin-options)

This module contains performance tests for the Spring Boot application using the [Gatling](https://gatling.io/) load testing tool.

## ⚙️ Dependencies

---

* [Gatling ~3.14](https://docs.gatling.io/tutorials/scripting-intro/)
* [JDK 25](https://openjdk.org/projects/jdk/25/)
* [Docker ~29](https://docs.docker.com/engine/release-notes/29/)

## 🏗️ Architecture

---

| File                                                                             | Description                          |
|----------------------------------------------------------------------------------|--------------------------------------|
| [run.sh](./run.sh)                                                               | Shell script for executing the tests |
| [index.html](./target/gatling/gatlinghighthroughputsimulation-latest/index.html) | HTML Gatling report                  |

## 🚀 Running the Tests

---

```bash
# Start the application and the database using Docker Compose
./../../deploy-orchestrator/start.sh
```
```bash
# Run the test using the provided shell script
./run.sh
```
```bash
# Stop the application and the database using Docker Compose
./../../deploy-orchestrator/stop.sh
```

## 🔎 Exploring plugin options

```bash
mvn gatling:help -Ddetail=true -Dgoal=test
```
