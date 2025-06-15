# Ignacio High-Throughput Java Application

[![Deploy](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/maven.yml/badge.svg)](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/maven.yml)
[![SonarQube](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/sonarqube.yml/badge.svg)](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/sonarqube.yml)
[![Dependencies](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/actions/workflows/dependency-review.yml)
[![License](https://img.shields.io/github/license/JuanPabloJimenezEsclusa/ignacio-high-throughput)](https://github.com/JuanPabloJimenezEsclusa/ignacio-high-throughput/LICENSE.md)

> • [Project Structure](#-project-structure)
  • [Technologies](#-technologies)
  • [Building Modules](#-building-modules)
  • [Running Modules](#-running-modules)
  • [API Endpoints](#-api-endpoints)
  • [Implementation Details](#-implementation-details)
  • [Profiling](#-profiling)
  • [Testing](#-testing)
  • [Performance Comparison](#-performance-comparison)
  • [References](#-references)

A comprehensive empirical study evaluating the performance characteristics and scalability patterns of high-throughput data processing architectures in Java, comparing traditional thread-per-request imperative models with event-loop reactive paradigms. 

This research implements controlled experimental scenarios across six distinct runtime configurations, analyzing the impact of virtual thread utilization and native compilation on response latency, resource efficiency, and throughput optimization. 

Quantitative performance metrics are systematically collected through industry-standard profiling tools and benchmarking methodologies to provide statistically significant insights into the architectural trade-offs between imperative and reactive programming models under varying computational loads.

## 📦 Project Structure

This is a multi-module project consisting of:

- [**imperative-throughput**](./imperative-throughput): Traditional Spring MVC implementation using blocking operations
- [**reactive-throughput**](./reactive-throughput): Spring WebFlux implementation using non-blocking reactive streams

## 🛠️ Technologies

[![OpenJDK](https://img.shields.io/badge/OpenJDK-24+-005571.svg)](https://adoptium.net/es/temurin/releases/?os=any&arch=any&version=24)
[![GraalVM](https://img.shields.io/badge/GraalVM-24+-005571.svg)](https://www.graalvm.org/jdk24/getting-started/linux/#script-friendly-urls)
[![Maven](https://img.shields.io/badge/Maven-3.9+-005571.svg)](https://maven.apache.org/ref/3.9.10/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14+-005571.svg)](https://docs.gradle.org/8.14.2/userguide/userguide.html)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring MVC](https://img.shields.io/badge/Spring%20MVC%20(imperative%20module)-6.2+-brightgreen.svg)](https://docs.spring.io/spring-framework/reference/web.html)
[![Spring WebFlux](https://img.shields.io/badge/Spring%20Webflux%20(reactive%20module)-6.2+-brightgreen.svg)](https://docs.spring.io/spring-framework/reference/web-reactive.html)

[![k6](https://img.shields.io/badge/K6-1.0+-orange.svg)](https://github.com/grafana/k6)
[![JMeter](https://img.shields.io/badge/JMeter-5.6+-orange.svg)](https://jmeter.apache.org/)
[![Gatling](https://img.shields.io/badge/Gatling-3.14+-orange.svg)](https://github.com/gatling/gatling)

[![Docker](https://img.shields.io/badge/Docker-28+-brown.svg)](https://www.docker.com/)
[![Docker-compose](https://img.shields.io/badge/Docker%20Compose-v2.35+-brown.svg)](https://docs.docker.com/compose/install/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.33+-brown.svg)](https://kubernetes.io/releases/)
[![AWS CLI](https://img.shields.io/badge/AWS%20CLI-2.27+-brown.svg)](https://aws.amazon.com/es/cli/)

## 🔨 Building modules

```bash
# Build all modules
mvn clean package
```
```bash
# Build all modules with Gradle
gradle clean build
```
```bash
export SPRING_PROFILES_ACTIVE="native"
# Build native artifacts
mvn -P"${SPRING_PROFILES_ACTIVE}" \
  native:compile \
  -Dmaven.test.skip=true \
  -Dmaven.build.cache.enabled=false \
  --projects imperative-throughput,reactive-throughput
```
```bash
gradle nativeBuild
```

- [Build Native Report - imperative-throughput](./imperative-throughput/target/imperative-throughput-build-report.html)
- [Build Native Report - reactive-throughput](./reactive-throughput/target/reactive-throughput-build-report.html)

```bash
export SPRING_PROFILES_ACTIVE="native"
# Build docker images
mvn -P"${SPRING_PROFILES_ACTIVE}" \
  spring-boot:build-image \
  -Dmaven.test.skip=true \
  -Dmaven.build.cache.enabled=false \
  --projects imperative-throughput,reactive-throughput
```
````bash
gradle bootBuildImage
````

## 🚀 Running modules

### Imperative Module

```bash
# Run with Maven
mvn spring-boot:run -f imperative-throughput/pom.xml
```
```bash
# Run with Gradle
gradle bootRun --project-dir imperative-throughput
```
```bash
# Run java -jar
# Include init profiling for recording during 10 minutes and dump when exit
java -XX:+FlightRecorder \
  -XX:StartFlightRecording=filename=resource-profiling/imperative-recording.jfr,settings=resource-profiling/custom.jfc,dumponexit=true,duration=10m \
  -jar ./imperative-throughput/target/imperative-throughput-1.0.0.jar
```
```bash
# Run native artifact
./imperative-throughput/target/imperative-throughput \
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=filename=resource-profiling/imperative-recording.jfr,settings=resource-profiling/custom.jfc,dumponexit=true,duration=10m
```
```bash
# Run native artifact built with Gradle
./imperative-throughput/build/native/nativeCompile/imperative-throughput
```
```bash
# Run docker image
docker run -it --rm \
  --memory="1024m" \
  --memory-reservation="0m" \
  --memory-swap="1024m" \
  --cpus="1.000" \
  --env BPL_JMX_ENABLED=true \
  --env BPL_JMX_PORT=5088 \
  --env BPL_JMX_RMI_PORT=5088 \
  --env BPL_JMX_HOST=0.0.0.0 \
  --env BPL_JMX_RMI_HOST=0.0.0.0 \
  --publish 8888:8888 \
  --publish 5088:5088 \
  --oom-kill-disable \
  imperative-throughput:1.0.0
```

The application will be available at `http://localhost:8888/imperative-throughput/`

### Reactive Module

```bash
# Run with Maven
mvn spring-boot:run -f reactive-throughput/pom.xml
```
```bash
# Run with Gradle
gradle bootRun --project-dir reactive-throughput
```
```bash
# Run java -jar
java -XX:+FlightRecorder \
  -XX:StartFlightRecording=filename=resource-profiling/reactive-recording.jfr,settings=resource-profiling/custom.jfc,dumponexit=true,duration=10m \
  -jar ./reactive-throughput/target/reactive-throughput-1.0.0.jar
```
```bash
# Run native artifact
./reactive-throughput/target/reactive-throughput \
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=filename=resource-profiling/reactive-recording.jfr,settings=resource-profiling/custom.jfc,dumponexit=true,duration=10m
```
```bash
# Run native artifact built with Gradle
./reactive-throughput/build/native/nativeCompile/reactive-throughput
```
```bash
# Run docker image
docker run -it --rm \
  --memory="1024m" \
  --memory-reservation="0m" \
  --memory-swap="1024m" \
  --cpus="1.000" \
  --env BPL_JMX_ENABLED=true \
  --env BPL_JMX_PORT=5099 \
  --env BPL_JMX_RMI_PORT=5099 \
  --env BPL_JMX_HOST=0.0.0.0 \
  --env BPL_JMX_RMI_HOST=0.0.0.0 \
  --publish 9999:9999 \
  --publish 5099:5099 \
  --oom-kill-disable \
  reactive-throughput:1.0.0
```

The application will be available at `http://localhost:9999/reactive-throughput/`

## 🔌 API Endpoints

### Imperative Module

- `GET /imperative-throughput/smokes` - Returns "OK" after a 300ms delay using blocking Thread#sleep

```bash
# Request endpoint
curl -sv http://localhost:8888/imperative-throughput/smokes
```
```bash
# Health check
curl -sv http://localhost:8888/imperative-throughput/actuator/health | jq
```
```bash
# Metrics
curl -sv http://localhost:8888/imperative-throughput/actuator/prometheus
curl -sv http://localhost:8888/imperative-throughput/actuator/metrics | jq
```

### Reactive Module

- `GET /reactive-throughput/smokes` - Returns "OK" after a 300ms delay using reactive non-blocking delay

```bash
# Request endpoint
curl -sv http://localhost:9999/reactive-throughput/smokes
```
```bash
# Health check
curl -sv http://localhost:9999/reactive-throughput/actuator/health | jq
```
```bash
# Metrics
curl -sv http://localhost:9999/reactive-throughput/actuator/prometheus
curl -sv http://localhost:9999/reactive-throughput/actuator/metrics | jq
```

## 🧪 Implementation Details

### Imperative Approach

Uses traditional Spring MVC with blocking operations:
- Thread.sleep for simulating work
- Synchronous request processing
- One thread per request model

### Reactive Approach

Uses Spring WebFlux with non-blocking operations:
- Mono/Flux reactive types
- Non-blocking delays
- Functional endpoint definitions

## 📊 Profiling

- [Diagnostic Command Tool (jcmd)](https://www.graalvm.org/jdk24/reference-manual/native-image/debugging-and-diagnostics/jcmd/)

```bash
# List JVM processes
jcmd -l
# Look for the imperative-throughput process
IMPERATIVE_PROCESS_ID="$(pgrep -f imperative-throughput-1.0.0.jar)"
# List available e
jcmd "${IMPERATIVE_PROCESS_ID}" help
# Dump the thread information to a file
jcmd "${IMPERATIVE_PROCESS_ID}" Thread.dump_to_file -format=json \
  -overwrite resource-profiling/imperative-throughput.hprof.json
```

```bash
# List JVM processes
jcmd -l
# Look for the reactive-throughput process
`REACTIVE_PROCESS_ID="$(pgrep -f reactive-throughput-1.0.0.jar)"`
# List available e
jcmd "${REACTIVE_PROCESS_ID}" help
# Dump the thread information to a file
jcmd "${REACTIVE_PROCESS_ID}" Thread.dump_to_file -format=json \
  -overwrite resource-profiling/reactive-throughput.hprof.json
```

- [JDK Flight Recording (jfr)](https://www.graalvm.org/latest/reference-manual/native-image/debugging-and-diagnostics/JFR/)

```bash
# Some events are not enabled by default
jfr configure --output resource-profiling/custom.jfc \
  jdk.VirtualThreadStart#enabled=true \
  jdk.VirtualThreadEnd#enabled=true
```

```bash
# Look for the imperative-throughput process
IMPERATIVE_PROCESS_ID="$(pgrep -f imperative-throughput-1.0.0.jar)"
# Start,dump,stop the recording
jcmd "${IMPERATIVE_PROCESS_ID}" JFR.start settings=resource-profiling/custom.jfc
jcmd "${IMPERATIVE_PROCESS_ID}" JFR.dump filename=resource-profiling/imperative-recording.jfr
jcmd "${IMPERATIVE_PROCESS_ID}" JFR.stop
# Summarize the recording
jfr summary resource-profiling/imperative-recording.jfr | grep -e ".*Thread.*"
jfr print --json --events all --recording resource-profiling/imperative-recording.jfr \
  > resource-profiling/imperative-recording.hprof.json
```

```bash
# Look for the reactive-throughput process
REACTIVE_PROCESS_ID="$(pgrep -f reactive-throughput-1.0.0.jar)"
# Start,dump,stop the recording
jcmd "${REACTIVE_PROCESS_ID}" JFR.start settings=resource-profiling/custom.jfc
jcmd "${REACTIVE_PROCESS_ID}" JFR.dump filename=resource-profiling/reactive-recording.jfr
jcmd "${REACTIVE_PROCESS_ID}" JFR.stop
# Summarize the recording
jfr summary resource-profiling/reactive-recording.jfr | grep -e ".*Thread.*"
jfr print --json --events all --recording resource-profiling/reactive-recording.jfr \
  > resource-profiling/reactive-recording.hprof.json
```

- [Java Mission Control (jmc)](https://github.com/openjdk/jmc)

```bash
# Download, extract and run
tar zxf jmc-9.1.0_linux-x64.tar.gz
./jmc-9.1.0_linux-x64/JDK\ Mission\ Control/jmc
```

| Imperative (native build)                                                         | Reactive (native build)                                                       |
|:----------------------------------------------------------------------------------|:------------------------------------------------------------------------------|
| ![summary](./resource-profiling/images/jmc/imperative/imperative-summary.png)     | ![summary](./resource-profiling/images/jmc/reactive/reactive-summary.png)     |
| ![memory](./resource-profiling/images/jmc/imperative/imperative-memory.png)       | ![memory](./resource-profiling/images/jmc/reactive/reactive-memory.png)       |
| ![gc](./resource-profiling/images/jmc/imperative/imperative-gc.png)               | ![gc](./resource-profiling/images/jmc/reactive/reactive-gc.png)               |
| ![processes](./resource-profiling/images/jmc/imperative/imperative-processes.png) | ![processes](./resource-profiling/images/jmc/reactive/reactive-processes.png) |

- [VisualVM](https://visualvm.github.io/)

```bash
# Download, extract and run
unzip visualvm_22.zip
./visualvm_22/bin/visualvm
```

| Imperative (jar)                                                                             | Reactive (jar)                                                                           |
|:---------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------|
| ![summary](./resource-profiling/images/visualvm/imperative/imperative-summary.png)           | ![summary](./resource-profiling/images/visualvm/reactive/reactive-summary.png)           |
| ![threads](./resource-profiling/images/visualvm/imperative/imperative-threads.png)           | ![threads](./resource-profiling/images/visualvm/reactive/reactive-threads.png)           |
| ![v-threads](./resource-profiling/images/visualvm/imperative/imperative-virtual-threads.png) | ![v-threads](./resource-profiling/images/visualvm/reactive/reactive-virtual-threads.png) |

## 🔍 Testing

Both modules include comprehensive test coverage including:
- Unit tests
- Loading tests
  - [K6](./testing/k6/Readme.md)
  - [JMeter](./testing/jmeter/Readme.md)
  - [Gatling](./testing/gatling/Readme.md)

Run unit tests with:

```bash
# Run all tests
mvn test && gradle test
```

## ⚡ Performance Comparison

This project allows comparing:

- Response times under load
- Resource utilization (CPU, memory)
- Throughput capabilities
- Scalability characteristics

between the traditional thread-per-request model and the reactive event-loop model.

### Java Performance Scenarios Comparison

Here's a table explaining 6 scenarios with performance indicators:

| # | Approach   | Virtual<br/>Threads | Native<br/>Build | Response Time<br/>by Thread | CPU<br/>Usage | Memory<br/>Usage | Startup<br/>Time | Learning<br/>Curve | 
|:--|:-----------|:--------------------|:-----------------|:----------------------------|:--------------|:-----------------|:-----------------|:-------------------|
| 1 | Imperative | ❌                   | ❌                | 🚨                          | 🚨            | 🚨               | 🚨               | 💪++               |
| 2 | Imperative | ✅                   | ❌                | 💪                          | 💪            | 💪+              | 🚨               | 💪+                |
| 3 | Imperative | ✅                   | ✅                | 💪+                         | 💪++          | 💪+              | 💪               | 💪                 |
| 4 | Reactive   | ❌                   | ❌                | 💪++                        | 💪+           | 💪+              | 🚨               | 💪                 |
| 5 | Reactive   | ❌                   | ✅                | 💪++                        | 💪++          | 💪+              | 💪               | 🚨                 |
| 6 | Reactive   | ✅                   | ✅                | 💪++                        | 💪++          | 💪++             | 💪               | 🚨--               |

#### Legend

- ✅ Enabled
- ❌ Not present
- 💪 Better 
- 🚨 Worse

#### Key Observations

- Native builds (scenarios 3, 5, 6) consistently show better memory usage and startup time
- Virtual threads improve performance in both programming models
- Imperative approach offers a better learning curve, especially without native compilation
- Reactive + (Virtual Threads or Native) (scenarios 5, 6) provides the best overall performance characteristics
- Imperative + Virtual Threads + Native (scenario 3) offers excellent performance with a simpler programming model

### Summary of K6 Tests

> - [K6 Web Dashboard](http://localhost:5665/ui/) (While the test is running)
> - [K6 Report](./testing/k6/result/summary.html)

| Scenario | Count | Rate   | Average | Maximum  | Median | Minimum | 90th Percentile | 95th Percentile |
|:---------|:------|:-------|:--------|:---------|:-------|:--------|:----------------|:----------------|
| 3        | 75462 | 359.03 | 812.49  | 48206.40 | 300.97 | 300.38  | 473.25          | 944.87          |
| 6        | 75462 | 359.03 | 309.74  | 600.70   | 301.11 | 300.40  | 328.08          | 357.04          |

| Imperative                                                             | Reactive                                                             |
|:-----------------------------------------------------------------------|:---------------------------------------------------------------------|
| ![K6 Imperative summary](./testing/k6/images/imperative-summary.png)   | ![K6 Imperative summary](./testing/k6/images/reactive-summary.png)   |
| ![K6 Imperative overview](./testing/k6/images/imperative-overview.png) | ![K6 Imperative overview](./testing/k6/images/reactive-overview.png) |
| ![K6 Imperative timing](./testing/k6/images/imperative-timing.png)     | ![K6 Imperative timing](./testing/k6/images/reactive-timing.png)     |

### Summary of JMeter Tests

> - [JMeter Report](./testing/jmeter/reports/html/index.html)

| Scenario | Executions<br/>Samples | FAIL | Error % | Response Times (ms)<br/>Average | Min | Max | Median | 90th pct | 95th pct | 99th pct | Throughput<br/>Transactional/s | Network (KB/sec)<br/>Received | Network (KB/sec)<br/>Sent |
|:---------|:-----------------------|:-----|:--------|:--------------------------------|:----|:----|:-------|:---------|:---------|:---------|:-------------------------------|:------------------------------|:--------------------------|
| 3        | 140000                 | 0    | 0.00%   | 304.44                          | 300 | 486 | 302.00 | 303.00   | 316.00   | 400.00   | 738.86                         | 183.27                        | 188.32                    |
| 6        | 140000                 | 0    | 0.00%   | 302.99                          | 300 | 382 | 302.00 | 302.00   | 311.00   | 335.00   | 737.24                         | 110.87                        | 186.47                    |

| Imperative                                                             | Reactive                                                             |
|:-----------------------------------------------------------------------|:---------------------------------------------------------------------|
| ![time-requests](./testing/jmeter/images/imperative-time-requests.png) | ![time-requests](./testing/jmeter/images/reactive-time-requests.png) |
| ![time-threads](./testing/jmeter/images/imperative-time-threads.png)   | ![time-threads](./testing/jmeter/images/reactive-time-threads.png)   |
| ![transactions](./testing/jmeter/images/imperative-transactions.png)   | ![transactions](./testing/jmeter/images/reactive-transactions.png)   |
### Summary of Gatling Tests

| Scenario | Executions<br/>Total | OK     | KO    | % KO  | Crit/s | Response Time (ms)<br/>Min | 50th pct | 75th pct | 95th pct | 99th pct | Max   | Mean | Std Dev |
|:---------|:---------------------|:-------|:------|:------|:-------|:---------------------------|:---------|:---------|:---------|:---------|:------|:-----|:--------|
| 3        | 120314               | 106749 | 13566 | 11.28 | 661.07 | 56                         | 307      | 363      | 14593    | 24284    | 29093 | 2401 | 4878    |
| 6        | 120314               | 107293 | 13016 | 10.82 | 661.07 | 13                         | 307      | 479      | 7143     | 7675     | 15082 | 1363 | 2339    |

| Imperative                                                                        | Reactive                                                                      |
|:----------------------------------------------------------------------------------|:------------------------------------------------------------------------------|
| ![Gatling Imperative Response](./testing/gatling/images/imperative-responses.png) | ![Gatling Reactive Response](./testing/gatling/images/reactive-responses.png) |

## 📚 References

--

- [Exploration of Java Virtual Threads and Performance Analysis](https://www.alibabacloud.com/blog/exploration-of-java-virtual-threads-and-performance-analysis_601860)
- [Why Do We Need Java Reactive Programming?](https://www.tatvasoft.com/blog/java-reactive-programming/)
- [Virtual threads in Java 21](https://luxoft-training.com/blog/virtual-threads-in-java-21)
- [Reactor WebFlux vs Virtual Threads](https://www.geeksforgeeks.org/reactor-webflux-vs-virtual-threads/)
- [How to Optimize Java Performance With Virtual Threads, Reactive Programming, and MongoDB](https://www.mongodb.com/developer/languages/java/virtual-threads-reactive-programming/)
- [Comparing Virtual Threads and Reactive Webflux in Spring](https://www.diva-portal.org/smash/get/diva2:1763111/FULLTEXT01.pdf)
