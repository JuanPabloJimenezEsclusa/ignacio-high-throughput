#!/usr/bin/env bash

# Example of usage:
#   ./run.sh
#   IMPERATIVE_URL=https://tech.jpje.xyz:443 REACTIVE_URL=https://tech.jpje.xyz:443 ./run.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

workspace="$(pwd)"

IMPERATIVE_URL="${IMPERATIVE_URL:-"http://imperative-throughput:8888"}/imperative-throughput/smokes"
REACTIVE_URL="${REACTIVE_URL:-"http://reactive-throughput:9999"}/reactive-throughput/smokes"

echo -e "${SEPARATOR} 🛠️ Test Configuration ${SEPARATOR}"
echo "IMPERATIVE_URL: ${IMPERATIVE_URL} | REACTIVE_URL: ${REACTIVE_URL}"

echo -e "${SEPARATOR} 🚀 Gatling Test Execution ${SEPARATOR}"
mvn clean gatling:test

echo -e "${SEPARATOR} 📊 Move the latest report to a separate directory ${SEPARATOR}"
mv ./target/gatling/gatlinghighthroughputsimulation-* ./target/gatling/gatlinghighthroughputsimulation-latest
