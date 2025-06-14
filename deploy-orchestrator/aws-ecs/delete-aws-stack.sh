#!/usr/bin/env bash

# Example of usage: ./delete-aws-stack.sh removeImages=true

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

parameter="${1:-"removeImages=false"}"
eval "${parameter}"
echo "removeImages: ${removeImages:-}"

__delete_ecs_stack() {
  echo -e "${SEPARATOR} 🗑️ Delete ecs stack. ${SEPARATOR}"
  echo "Init ${FUNCNAME:-} ..."

  aws cloudformation delete-stack \
    --no-cli-auto-prompt \
    --no-cli-pager \
    --stack-name "high-throughput-services-stack"

  # Wait for ecs stack to be deleted
  echo "Waiting ${FUNCNAME:-} ..."
  aws cloudformation wait stack-delete-complete \
    --stack-name "high-throughput-services-stack"

  echo "End ${FUNCNAME:-} successfully!"
}

__delete_log_groups() {
  echo -e "${SEPARATOR} 🗑️ Delete log groups. ${SEPARATOR}"

  local log_groups=(
    "/aws/ecs/containerinsights/high-throughput-cluster/performance"
    "/vpc/high-throughput-services-stack"
    "/ecs/imperative-throughput"
    "/ecs/reactive-throughput"
  )

  for log_group in "${log_groups[@]}"; do
    echo "Deleting log group: ${log_group}"
    aws logs delete-log-group \
      --log-group-name "${log_group}" \
      --no-cli-auto-prompt \
      --no-cli-pager 2>/dev/null || echo "Log group not found or already deleted: ${log_group}"
  done
}

__cleanUp() {
  echo -e "${SEPARATOR} 🧹 Clean up ${SEPARATOR}"
  docker volume prune --force --all

  if [[ "${removeImages:-}" == "true" ]]; then
    docker images --filter reference='*/*/*-throughput' --format '{{.Repository}}:{{.Tag}}' | xargs -I {} docker rmi -f {}
    docker images --filter reference='*-throughput' --format '{{.Repository}}:{{.Tag}}' | xargs -I {} docker rmi -f {}
  else
    echo -e "🚧 Skip remove images"
  fi
}

# Main script
main() {
  echo "Init ${0##*/} (${FUNCNAME:-})"
  __delete_ecs_stack
  __delete_log_groups
  __cleanUp
  echo "Done ${0##*/} (${FUNCNAME:-})"
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main
