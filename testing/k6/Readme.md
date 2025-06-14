# K6 Testing

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Running the Tests](#-running-the-tests)

This module contains performance tests for the Spring Boot application using the [K6](https://k6.io/) load testing tool.

## ⚙️ Dependencies

---

* K6 >= 1.0.0
* Docker >= 28
* Docker Compose >= v2.35

## 🏗️ Architecture

---

| File                                                                    | Description                          |
|-------------------------------------------------------------------------|--------------------------------------|
| [high-throughput-load-tests.js](./script/high-throughput-load-tests.js) | Test plan file                       |
| [summary.html](./result/summary.html)                                   | HTML summary report                  |
| [run.sh](./run.sh)                                                      | Shell script for executing the tests |

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
