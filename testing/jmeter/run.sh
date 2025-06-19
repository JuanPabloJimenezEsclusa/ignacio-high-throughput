#!/usr/bin/env bash

# Example of usage:
#   THREADS=10000 RAMP_UP=10 LOOPS=30 ./run.sh
#   THREADS=15000 RAMP_UP=20 LOOPS=10 BASE_URL_PROTOCOL=https BASE_URL=tech.jpje.xyz BASE_IMPERATIVE_URL_PORT=443 BASE_REACTIVE_URL_PORT=443 ./run.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

JMETER_TEST_PATH="${JMETER_TEST_PATH:-"."}" # This variable defines the path to the JMeter test plan configuration
THREADS="${THREADS:-7000}" # This variable sets the number of concurrent users (threads) to simulate during the test
RAMP_UP="${RAMP_UP:-20}" # This variable specifies the duration (in seconds) for gradually increasing the load from 0 to the specified number of users
LOOPS="${LOOPS:-20}" # This variable defines the total number of times to iterate through the test

BASE_URL_PROTOCOL="${BASE_URL_PROTOCOL:-"http"}"
BASE_URL="${BASE_URL:-"localhost"}"
BASE_IMPERATIVE_URL_PORT="${BASE_IMPERATIVE_URL_PORT:-"8888"}"
BASE_REACTIVE_URL_PORT="${BASE_REACTIVE_URL_PORT:-"9999"}"

echo -e "${SEPARATOR} 🛠️ Test Configuration ${SEPARATOR}"
echo "JMETER_TEST_PATH: ${JMETER_TEST_PATH} | THREADS: ${THREADS} | RAMP_UP: ${RAMP_UP} | LOOPS: ${LOOPS}"
echo "BASE_URL: ${BASE_URL_PROTOCOL}://${BASE_URL}"

echo -e "${SEPARATOR} 🧹 Test Workspace Cleanup ${SEPARATOR}"
rm -dr "${JMETER_TEST_PATH}/reports" || true
rm -dr "${JMETER_TEST_PATH}/jmeter.log" || true

echo -e "${SEPARATOR} 🚀 JMeter Test Execution ${SEPARATOR}"
time jmeter -n \
  -t "${JMETER_TEST_PATH}/high-throughput-performance.jmx" \
  -JTHREADS="${THREADS}" \
  -JRAMP_UP="${RAMP_UP}" \
  -JLOOPS="${LOOPS}" \
  -JBASE_URL_PROTOCOL="${BASE_URL_PROTOCOL}" \
  -JBASE_URL="${BASE_URL}" \
  -JBASE_IMPERATIVE_URL_PORT="${BASE_IMPERATIVE_URL_PORT}" \
  -JBASE_REACTIVE_URL_PORT="${BASE_REACTIVE_URL_PORT}" \
  -l "${JMETER_TEST_PATH}/reports/result.csv"

echo -e "${SEPARATOR} 📊 JMeter HTML Report Generation ${SEPARATOR}"
time jmeter \
  -g "${JMETER_TEST_PATH}/reports/result.csv" \
  -o "${JMETER_TEST_PATH}/reports/html"
