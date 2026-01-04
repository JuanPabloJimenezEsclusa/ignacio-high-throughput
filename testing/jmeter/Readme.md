# Jmeter testing

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Running the Tests](#-running-the-tests)

Performance testing project using JMeter

## ⚙️ Dependencies

---

* [JDK 25](https://openjdk.org/projects/jdk/25/)
* [JMeter ~5.6](https://jmeter.apache.org/changes.html)

## 🏗️ Architecture

---

| File                                                                 | Description                         |
|----------------------------------------------------------------------|-------------------------------------|
| [high-throughput-performance.jmx](./high-throughput-performance.jmx) | Test plan file                      |
| [run.sh](./run.sh)                                                   | Shell script for executing the test |
| [index.html](./reports/html/index.html)                              | HTML summary report                 |

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
