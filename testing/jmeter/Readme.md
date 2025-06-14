# Jmeter testing

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Running the Tests](#-running-the-tests)

Performance testing project using JMeter

## ⚙️ Dependencies

---

* [JDK >= 24+](https://openjdk.org/projects/jdk/24/)
* JMeter >= 5.6+

## 🏗️ Architecture

---

| File                            | Description                         |
|---------------------------------|-------------------------------------|
| high-throughput-performance.jmx | Test plan file                      |
| run.sh                          | Shell script for executing the test |

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
