#!/usr/bin/env bash

# Example of usage:
#   THREADS=2000 RAMP_UP=10 LOOPS=30 ./run.sh
#   ENVIRONMENT=aws RUNS=3 THREADS=5000 RAMP_UP=20 LOOPS=10 ./run.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

JMETER_TEST_PATH="${JMETER_TEST_PATH:-"."}" # This variable defines the path to the JMeter test plan configuration
THREADS="${THREADS:-7000}" # This variable sets the number of concurrent users (threads) to simulate during the test
RAMP_UP="${RAMP_UP:-20}" # This variable specifies the duration (in seconds) for gradually increasing the load from 0 to the specified number of users
LOOPS="${LOOPS:-20}" # This variable defines the total number of times to iterate through the test

ENVIRONMENT="${ENVIRONMENT:-dev}"
RUNS="${RUNS:-1}"
DRY_RUN="${DRY_RUN:-false}"
TOOL="jmeter"

case "${ENVIRONMENT}" in
  dev)
    ENV_PROTOCOL="http"
    ENV_HOST="localhost"
    ENV_IMPERATIVE_PORT="8888"
    ENV_REACTIVE_PORT="9999"
    ;;
  aws)
    ENV_PROTOCOL="https"
    ENV_HOST="tech.jpje.net"
    ENV_IMPERATIVE_PORT="443"
    ENV_REACTIVE_PORT="443"
    ;;
  *)
    echo "Unsupported ENVIRONMENT '${ENVIRONMENT}'. Use 'dev' or 'aws'." >&2
    exit 64
    ;;
esac

if ! [[ "${RUNS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "RUNS must be a positive integer, got '${RUNS}'." >&2
  exit 64
fi

# Explicit per-tool URL overrides win over the environment default.
BASE_URL_PROTOCOL="${BASE_URL_PROTOCOL:-${ENV_PROTOCOL}}"
BASE_URL="${BASE_URL:-${ENV_HOST}}"
BASE_IMPERATIVE_URL_PORT="${BASE_IMPERATIVE_URL_PORT:-${ENV_IMPERATIVE_PORT}}"
BASE_REACTIVE_URL_PORT="${BASE_REACTIVE_URL_PORT:-${ENV_REACTIVE_PORT}}"
IMPERATIVE_TARGET="${BASE_URL_PROTOCOL}://${BASE_URL}:${BASE_IMPERATIVE_URL_PORT}/imperative-throughput"
REACTIVE_TARGET="${BASE_URL_PROTOCOL}://${BASE_URL}:${BASE_REACTIVE_URL_PORT}/reactive-throughput"

echo -e "${SEPARATOR} 🛠️ Test Configuration ${SEPARATOR}"
echo "ENVIRONMENT: ${ENVIRONMENT} | RUNS: ${RUNS}"
echo "JMETER_TEST_PATH: ${JMETER_TEST_PATH} | THREADS: ${THREADS} | RAMP_UP: ${RAMP_UP} | LOOPS: ${LOOPS}"
echo "IMPERATIVE_TARGET: ${IMPERATIVE_TARGET} | REACTIVE_TARGET: ${REACTIVE_TARGET}"

write_metadata() {
  local run_id="$1"
  local run_dir="$2"
  mkdir -p "${run_dir}"
  {
    echo "ENVIRONMENT=${ENVIRONMENT}"
    echo "TOOL=${TOOL}"
    echo "RUN=${run_id}"
    echo "IMPERATIVE_TARGET=${IMPERATIVE_TARGET}"
    echo "REACTIVE_TARGET=${REACTIVE_TARGET}"
    echo "GIT_SHA=$(git rev-parse --short HEAD)"
    echo "TIMESTAMP_UTC=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "CLIENT_CPUS=$(getconf _NPROCESSORS_ONLN 2>/dev/null || nproc)"
  } > "${run_dir}/metadata.txt"
}

run_once() {
  local run_id="$1"
  local run_dir="${JMETER_TEST_PATH}/reports/${ENVIRONMENT}/run-${run_id}"

  echo -e "${SEPARATOR} 🚀 JMeter Test Execution (run ${run_id}/${RUNS}) ${SEPARATOR}"
  echo "Report directory: ${run_dir}"

  # Only the target run directory is reset; committed reports are preserved.
  rm -rf "${run_dir}" || true
  mkdir -p "${run_dir}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] metadata.txt would contain:"
    write_metadata "${run_id}" "${run_dir}"
    cat "${run_dir}/metadata.txt"
    echo "[DRY_RUN] skipping JMeter execution"
    return 0
  fi

  write_metadata "${run_id}" "${run_dir}"

  time jmeter -n \
    -t "${JMETER_TEST_PATH}/high-throughput-performance.jmx" \
    -JTHREADS="${THREADS}" \
    -JRAMP_UP="${RAMP_UP}" \
    -JLOOPS="${LOOPS}" \
    -JBASE_URL_PROTOCOL="${BASE_URL_PROTOCOL}" \
    -JBASE_URL="${BASE_URL}" \
    -JBASE_IMPERATIVE_URL_PORT="${BASE_IMPERATIVE_URL_PORT}" \
    -JBASE_REACTIVE_URL_PORT="${BASE_REACTIVE_URL_PORT}" \
    -j "${run_dir}/jmeter.log" \
    -l "${run_dir}/result.csv"

  echo -e "${SEPARATOR} 📊 JMeter HTML Report Generation ${SEPARATOR}"
  # The HTML output directory also contains statistics.json (machine-readable per-run metrics).
  time jmeter \
    -g "${run_dir}/result.csv" \
    -o "${run_dir}/html"
}

for run_id in $(seq 1 "${RUNS}"); do
  run_once "${run_id}"
done
