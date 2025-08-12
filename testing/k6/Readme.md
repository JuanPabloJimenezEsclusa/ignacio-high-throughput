# K6 Testing

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Running the Tests](#-running-the-tests)

This module contains performance tests for the Spring Boot application using the [K6](https://k6.io/) load testing tool.

## ⚙️ Dependencies

---

* [K6 ~0.57](https://grafana.com/docs/k6/next/release-notes/)
* [Docker ~28](https://docs.docker.com/engine/release-notes/28/)

## 🏗️ Architecture

---

| File                                                                    | Description                          |
|-------------------------------------------------------------------------|--------------------------------------|
| [high-throughput-load-tests.js](./script/high-throughput-load-tests.js) | Test plan file                       |
| [run.sh](./run.sh)                                                      | Shell script for executing the tests |
| [summary.html](./result/summary.html)                                   | HTML summary report                  |

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
