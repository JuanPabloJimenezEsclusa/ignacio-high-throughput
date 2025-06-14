#!/usr/bin/env bash

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

__cleanUp() {
  echo -e "${SEPARATOR} 🗑️ Delete the kind cluster. ${SEPARATOR}"
  kind delete cluster --name kind-cluster

  echo -e "${SEPARATOR} 🗑️ Delete tmp folders. ${SEPARATOR}"
  sudo rm -rdf /tmp/shared/ /tmp/hostpath-provisioner/ || true

  echo -e "${SEPARATOR} 🧹 Prune docker. ${SEPARATOR}"
  docker system prune --volumes --force || true
}

main() {
  __cleanUp
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main
