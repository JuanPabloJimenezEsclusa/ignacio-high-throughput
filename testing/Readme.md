# Testing conventions

Three runners: `k6/run.sh`, `jmeter/run.sh` and `gatling/run.sh`. All three accept the same variables.

* `ENVIRONMENT`: `dev` (default) or `aws`.
  * `dev`: imperative at `http://localhost:8888` and reactive at `http://localhost:9999`.
  * `aws`: both modules at `https://tech.jpje.net:443`.
  * An explicit per-tool URL overrides the environment default.
* `RUNS`: the number of repetitions. The default is `1`.

## Report layout

    testing/<tool>/{result|reports}/${ENVIRONMENT}/run-${i}/

Each run writes `metadata.txt` (`ENVIRONMENT`, `TOOL`, `RUN`, `IMPERATIVE_TARGET`, `REACTIVE_TARGET`, `GIT_SHA`, `TIMESTAMP_UTC`, `CLIENT_CPUS`) and the machine-readable report of the tool: `summary.json` for k6, `html/statistics.json` for JMeter, and a copy of the report for Gatling.

## Aggregate

    ./aggregate.sh <k6|jmeter|gatling> <dev|aws>

The script writes `aggregate.json` next to the runs. The file holds `median_ms`, `p95_ms`, `min_ms` and `max_ms` for each implementation, plus the run count.
