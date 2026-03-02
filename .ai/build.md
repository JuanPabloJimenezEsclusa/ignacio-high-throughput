# Build & Run

## Build

### Maven

```bash
mvn clean install                                        # all modules
mvn clean install -f imperative-throughput/pom.xml       # single module
mvn clean install -DskipTests                            # skip tests
mvn rewrite:runNoFork -Popen-rewrite                     # format code
mvn generate-sources                                     # generate changelog
```

### Gradle

```bash
gradle clean build                                       # all modules
gradle :imperative-throughput:build                      # single module
gradle build -x test                                     # skip tests
```

## Run

### Imperative — `http://localhost:8888/imperative-throughput/`

```bash
mvn spring-boot:run -f imperative-throughput/pom.xml
gradle :imperative-throughput:bootRun
java -jar imperative-throughput/target/imperative-throughput-1.0.0.jar
```

### Reactive — `http://localhost:9999/reactive-throughput/`

```bash
mvn spring-boot:run -f reactive-throughput/pom.xml
gradle :reactive-throughput:bootRun
java -jar reactive-throughput/target/reactive-throughput-1.0.0.jar
```
