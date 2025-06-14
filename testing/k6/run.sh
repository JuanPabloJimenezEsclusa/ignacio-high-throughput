#!/usr/bin/env bash

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

workspace="$(pwd)"

# K6 Test Execution
# https://grafana.com/docs/k6/latest/set-up/install-k6/?src=k6io&pg=oss-k6&plcmt=deploy-box-1#docker
echo -e "${SEPARATOR} 🚀 K6 Test Execution ${SEPARATOR}"
time docker run --rm \
  --name k6-load-test \
  --env K6_WEB_DASHBOARD=true \
  --env K6_WEB_DASHBOARD_HOST=localhost \
  --env K6_WEB_DASHBOARD_PORT=5665 \
  --env K6_WEB_DASHBOARD_PERIOD=5s \
  --volume "${workspace}/script":/scripts:rw \
  --volume "${workspace}/result":/result:rw \
  --network host \
  -i grafana/k6:master-with-browser run /scripts/high-throughput-load-tests.js
