#!/usr/bin/env bash

# Example of usage:
#   ./run.sh
#   ENVIRONMENT=aws RUNS=3 ./run.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

workspace="$(pwd)"

ENVIRONMENT="${ENVIRONMENT:-dev}"
RUNS="${RUNS:-1}"
DRY_RUN="${DRY_RUN:-false}"
TOOL="gatling"

case "${ENVIRONMENT}" in
  dev)
    ENV_IMPERATIVE_URL="http://localhost:8888"
    ENV_REACTIVE_URL="http://localhost:9999"
    ;;
  aws)
    ENV_IMPERATIVE_URL="https://tech.jpje.net:443"
    ENV_REACTIVE_URL="https://tech.jpje.net:443"
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

# The simulation reads IMPERATIVE_BASE_URL / REACTIVE_BASE_URL (see GatlingHighThroughputSimulation).
# Resolution priority: IMPERATIVE_BASE_URL (full base, wins) > IMPERATIVE_URL (legacy host base) > environment default.
if [[ -n "${IMPERATIVE_BASE_URL:-}" ]]; then
  IMPERATIVE_TARGET="${IMPERATIVE_BASE_URL}"
elif [[ -n "${IMPERATIVE_URL:-}" ]]; then
  IMPERATIVE_TARGET="${IMPERATIVE_URL%/}/imperative-throughput"
else
  IMPERATIVE_TARGET="${ENV_IMPERATIVE_URL}/imperative-throughput"
fi

if [[ -n "${REACTIVE_BASE_URL:-}" ]]; then
  REACTIVE_TARGET="${REACTIVE_BASE_URL}"
elif [[ -n "${REACTIVE_URL:-}" ]]; then
  REACTIVE_TARGET="${REACTIVE_URL%/}/reactive-throughput"
else
  REACTIVE_TARGET="${ENV_REACTIVE_URL}/reactive-throughput"
fi

export IMPERATIVE_BASE_URL="${IMPERATIVE_TARGET}"
export REACTIVE_BASE_URL="${REACTIVE_TARGET}"

echo -e "${SEPARATOR} 🛠️ Test Configuration ${SEPARATOR}"
echo "ENVIRONMENT: ${ENVIRONMENT} | RUNS: ${RUNS}"
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
  local run_dir="${workspace}/reports/${ENVIRONMENT}/run-${run_id}"

  echo -e "${SEPARATOR} 🚀 Gatling Test Execution (run ${run_id}/${RUNS}) ${SEPARATOR}"
  echo "Report directory: ${run_dir}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] metadata.txt would contain:"
    write_metadata "${run_id}" "${run_dir}"
    cat "${run_dir}/metadata.txt"
    echo "[DRY_RUN] skipping Gatling execution"
    return 0
  fi

  write_metadata "${run_id}" "${run_dir}"

  mvn clean gatling:test

  echo -e "${SEPARATOR} 📊 Move the latest report to a separate directory ${SEPARATOR}"
  mv ./target/gatling/gatlinghighthroughputsimulation-* ./target/gatling/gatlinghighthroughputsimulation-latest

  echo -e "${SEPARATOR} 📦 Copy the report into the run directory ${SEPARATOR}"
  cp -a ./target/gatling/gatlinghighthroughputsimulation-latest/. "${run_dir}/"
}

for run_id in $(seq 1 "${RUNS}"); do
  run_once "${run_id}"
done
