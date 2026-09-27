# Testing conventions

Runners: `k6/run.sh`, `jmeter/run.sh`, `gatling/run.sh`. All accept:

- `ENVIRONMENT` — `dev` (default) or `aws`.
  - `dev`: imperative `http://localhost:8888`, reactive `http://localhost:9999`
  - `aws`: imperative + reactive `https://tech.jpje.net:443`
  - Explicit per-tool URL overrides still win.
- `RUNS` — repetitions, default `1`.

## Report layout

    testing/<tool>/{result|reports}/${ENVIRONMENT}/run-${i}/

Each run writes `metadata.txt` (`ENVIRONMENT`, `TOOL`, `RUN`, `IMPERATIVE_TARGET`,
`REACTIVE_TARGET`, `GIT_SHA`, `TIMESTAMP_UTC`, `CLIENT_CPUS`) and the tool's machine-readable
report: `summary.json` (k6), `html/statistics.json` (jmeter), copied report (gatling).

## Aggregate

    ./aggregate.sh <k6|jmeter|gatling> <dev|aws>

Writes `aggregate.json` beside the runs with `median_ms`, `p95_ms`, `min_ms`, `max_ms` per
implementation and the run count.
