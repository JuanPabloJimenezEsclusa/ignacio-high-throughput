#!/usr/bin/env bash

# Example of usage:
#   ./run.sh
#   ENVIRONMENT=aws RUNS=3 ./run.sh
#   ENVIRONMENT=dev IMPERATIVE_THROUGHPUT_URL=http://localhost:8888 REACTIVE_THROUGHPUT_URL=http://localhost:9999 ./run.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

workspace="$(pwd)"

ENVIRONMENT="${ENVIRONMENT:-dev}"
RUNS="${RUNS:-1}"
DRY_RUN="${DRY_RUN:-false}"
TOOL="k6"

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

# Explicit per-tool URL overrides win over the environment default.
IMPERATIVE_BASE="${IMPERATIVE_THROUGHPUT_URL:-${ENV_IMPERATIVE_URL}}"
REACTIVE_BASE="${REACTIVE_THROUGHPUT_URL:-${ENV_REACTIVE_URL}}"
IMPERATIVE_TARGET="${IMPERATIVE_BASE}/imperative-throughput"
REACTIVE_TARGET="${REACTIVE_BASE}/reactive-throughput"

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
  local run_dir="${workspace}/result/${ENVIRONMENT}/run-${run_id}"

  echo -e "${SEPARATOR} 🚀 K6 Test Execution (run ${run_id}/${RUNS}) ${SEPARATOR}"
  echo "Report directory: ${run_dir}"

  if [[ "${DRY_RUN}" == "true" ]]; then
    echo "[DRY_RUN] metadata.txt would contain:"
    write_metadata "${run_id}" "${run_dir}"
    cat "${run_dir}/metadata.txt"
    echo "[DRY_RUN] skipping k6 execution"
    return 0
  fi

  write_metadata "${run_id}" "${run_dir}"

  # K6 Test Execution
  # https://grafana.com/docs/k6/latest/set-up/install-k6/?src=k6io&pg=oss-k6&plcmt=deploy-box-1#docker
  time docker run --rm \
    --name "k6-load-test-${ENVIRONMENT}-${run_id}" \
    --env K6_WEB_DASHBOARD=true \
    --env K6_WEB_DASHBOARD_HOST=localhost \
    --env K6_WEB_DASHBOARD_PORT=5665 \
    --env K6_WEB_DASHBOARD_PERIOD=5s \
    --env K6_SUMMARY_HTML="/result/${ENVIRONMENT}/run-${run_id}/summary.html" \
    --env IMPERATIVE_THROUGHPUT_URL="${IMPERATIVE_TARGET}" \
    --env REACTIVE_THROUGHPUT_URL="${REACTIVE_TARGET}" \
    --volume "${workspace}/script":/scripts:rw \
    --volume "${workspace}/result":/result:rw \
    --network host \
    -i grafana/k6:master-with-browser run \
    --summary-export "/result/${ENVIRONMENT}/run-${run_id}/summary.json" \
    /scripts/high-throughput-load-tests.js
}

for run_id in $(seq 1 "${RUNS}"); do
  run_once "${run_id}"
done
