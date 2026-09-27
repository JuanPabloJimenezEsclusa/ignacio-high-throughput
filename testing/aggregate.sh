#!/usr/bin/env bash

# Aggregates every run of a given tool + environment into a single machine-readable file.
#
# Usage: ./aggregate.sh <k6|jmeter|gatling> <dev|aws>
#
# Output:
#   k6      -> testing/k6/result/<env>/aggregate.json
#   jmeter  -> testing/jmeter/reports/<env>/aggregate.json
#   gatling -> testing/gatling/reports/<env>/aggregate.json
#
# Aggregation rule: each run already reports a median/p95/min/max per implementation.
# Across N runs we take the median of the per-run medians, the median of the per-run p95s,
# the minimum of the per-run mins and the maximum of the per-run maxes.

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

usage() {
  echo "Usage: $0 <k6|jmeter|gatling> <dev|aws>" >&2
  exit 64
}

TOOL="${1:-}"
ENVIRONMENT="${2:-}"
[[ -n "${TOOL}" && -n "${ENVIRONMENT}" ]] || usage
case "${TOOL}" in k6|jmeter|gatling) ;; *) usage ;; esac
case "${ENVIRONMENT}" in dev|aws) ;; *) usage ;; esac

command -v jq >/dev/null 2>&1 || { echo "jq is required but not installed." >&2; exit 69; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "${TOOL}" in
  k6)
    INPUT_DIR="${SCRIPT_DIR}/k6/result/${ENVIRONMENT}"
    METRICS_SOURCE="summary.json (metrics[\"http_req_duration{impl:imperative|reactive}\"].values)"
    ;;
  jmeter)
    INPUT_DIR="${SCRIPT_DIR}/jmeter/reports/${ENVIRONMENT}"
    METRICS_SOURCE="statistics.json (per-transaction medianResTime/pct2ResTime[95th]/minResTime/maxResTime)"
    ;;
  gatling)
    INPUT_DIR="${SCRIPT_DIR}/gatling/reports/${ENVIRONMENT}"
    METRICS_SOURCE="js/stats.json (not emitted by Gatling OSS 3.15.1 - see notes)"
    ;;
esac

OUTPUT_FILE="${INPUT_DIR}/aggregate.json"

if [[ ! -d "${INPUT_DIR}" ]]; then
  echo "No runs found: directory '${INPUT_DIR}' does not exist." >&2
  exit 66
fi

mapfile -t RUN_DIRS < <(find "${INPUT_DIR}" -mindepth 1 -maxdepth 1 -type d -name 'run-*' | sort -V)
if [[ "${#RUN_DIRS[@]}" -eq 0 ]]; then
  echo "No run-* directories found under '${INPUT_DIR}'." >&2
  exit 66
fi

echo -e "${SEPARATOR} 📊 Aggregate ${TOOL} / ${ENVIRONMENT} (${#RUN_DIRS[@]} run(s)) ${SEPARATOR}"
echo "Source: ${METRICS_SOURCE}"

RUNS_TMP="$(mktemp)"
NOTES_TMP="$(mktemp)"
trap 'rm -f "${RUNS_TMP}" "${NOTES_TMP}"' EXIT

add_note() {
  jq -cn --arg n "$1" '$n' >> "${NOTES_TMP}"
}

# k6 summary-export: metrics are keyed by metric name; custom tags produce sub-metrics such as
# "http_req_duration{impl:imperative}". Trend values expose med/min/max/p(95).
extract_k6() {
  jq -c '
    def impl($k):
      (.metrics[$k].values) as $v
      | if $v == null then null
        else {med: $v.med, p95: $v["p(95)"], min: $v.min, max: $v.max} end;
    {imperative: impl("http_req_duration{impl:imperative}"),
     reactive:   impl("http_req_duration{impl:reactive}")}
  ' "$1"
}

# JMeter dashboard statistics.json is keyed by transaction label (plus a "Total" entry).
# pct2ResTime is the 95th percentile under JMeter's default percentiles (pct1=90, pct2=95, pct3=99).
extract_jmeter() {
  jq -c '
    def impl($needle):
      ([to_entries[] | select((.key | ascii_downcase) | contains($needle))] | first) as $e
      | if $e == null then null
        else {med: $e.value.medianResTime, p95: $e.value.pct2ResTime,
              min: $e.value.minResTime, max: $e.value.maxResTime} end;
    {imperative: impl("imperative"), reactive: impl("reactive")}
  ' "$1"
}

for run_dir in "${RUN_DIRS[@]}"; do
  run_name="$(basename "${run_dir}")"
  per_run='{"imperative":null,"reactive":null}'

  case "${TOOL}" in
    k6)
      if [[ -f "${run_dir}/summary.json" ]]; then
        per_run="$(extract_k6 "${run_dir}/summary.json")"
      else
        echo "WARN: ${run_name}: missing summary.json" >&2
      fi
      ;;
    jmeter)
      stats_file=""
      if [[ -f "${run_dir}/statistics.json" ]]; then
        stats_file="${run_dir}/statistics.json"
      elif [[ -f "${run_dir}/html/statistics.json" ]]; then
        stats_file="${run_dir}/html/statistics.json"
      fi
      if [[ -n "${stats_file}" ]]; then
        per_run="$(extract_jmeter "${stats_file}")"
      else
        echo "WARN: ${run_name}: missing statistics.json" >&2
      fi
      ;;
    gatling)
      # Gatling OSS 3.15.1 (pinned in testing/gatling/pom.xml) writes no js/stats.json:
      # its statistics are embedded in the HTML report and simulation.log is a binary format.
      # No machine-readable per-implementation JSON exists, so no mapping is invented here.
      if [[ -f "${run_dir}/js/stats.json" ]]; then
        echo "WARN: ${run_name}: js/stats.json present but its schema is not established from any committed artifact; ignoring it rather than inventing field names." >&2
      fi
      ;;
  esac

  jq -cn --argjson r "${per_run}" '$r' >> "${RUNS_TMP}"
done

if [[ "${TOOL}" == "gatling" ]]; then
  add_note "Gatling OSS 3.15.1 emits no js/stats.json; performance statistics are embedded in the HTML report and simulation.log is binary. Per-implementation percentiles cannot be derived from a JSON artifact, so imperative/reactive are null. Pin the schema here if a future Gatling version emits stats.json."
fi
add_note "Aggregation across runs: median of per-run medians, median of per-run p95s, min of per-run mins, max of per-run maxes."

if [[ "${#RUN_DIRS[@]}" -gt 1 && "${TOOL}" == "k6" ]]; then
  add_note "k6 per-implementation values are read from metrics[\"http_req_duration{impl:imperative}\"] / [\"...{impl:reactive}\"]. If a run's summary-export lacks those submetrics, that run contributes null."
fi

jq -s \
  --arg tool "${TOOL}" \
  --arg env "${ENVIRONMENT}" \
  --arg source "${METRICS_SOURCE}" \
  --slurpfile notes "${NOTES_TMP}" \
  '
  def mednum:
    sort as $s
    | ($s | length) as $n
    | if $n == 0 then null
      elif ($n % 2) == 1 then $s[($n / 2) | floor]
      else (($s[($n / 2 - 1) | floor] + $s[($n / 2) | floor]) / 2)
      end;
  def agg($key):
    [ .[][$key] | select(. != null) ] as $vals
    | if ($vals | length) == 0 then null
      else
        { median_ms: ([$vals[].med] | mednum),
          p95_ms:    ([$vals[].p95] | mednum),
          min_ms:    ([$vals[].min] | min),
          max_ms:    ([$vals[].max] | max) }
      end;
  . as $runs
  | { tool: $tool,
      environment: $env,
      runs: ($runs | length),
      imperative: ($runs | agg("imperative")),
      reactive:   ($runs | agg("reactive")),
      source: $source,
      notes: $notes }
  ' "${RUNS_TMP}" > "${OUTPUT_FILE}"

echo "Wrote ${OUTPUT_FILE}"
cat "${OUTPUT_FILE}"
