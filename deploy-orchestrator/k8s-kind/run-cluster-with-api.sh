#!/usr/bin/env bash

# Example of usage: ./run-cluster-with-api.sh

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

__installClients() {
  echo -e "${SEPARATOR} 📦 Install clients. ${SEPARATOR}"
  cd cluster
  ./install-clients.sh
}

__startCluster() {
  echo -e "${SEPARATOR} 🚀 Start cluster. ${SEPARATOR}"
  ./start-cluster.sh
}

__installAPI() {
  echo -e "${SEPARATOR} 🚀 Install API. ${SEPARATOR}"
  cd ../API
  ./apply.sh buildProjects=true
}

main() {
  __installClients
  __startCluster
  __installAPI
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main
