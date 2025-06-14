#!/usr/bin/env bash

# Example of usage: ./apply.sh buildProjects=true

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

parameter="${1:-"buildProjects=false"}"
eval "${parameter}"
echo "buildProjects: ${buildProjects:-}"

workspace="$(pwd)"
baseProjectPath="../../../"

# Environment variables
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-"native"}"

__removeImages() {
  if [[ "${buildProjects:-}" == "true" ]]; then
    echo -e "${SEPARATOR} 🗑️ Remove image. ${SEPARATOR}"
    docker rmi docker.io/library/imperative-throughput:1.0.0 \
      docker.io/library/reactive-throughput:1.0.0 || true
  else
    echo -e "🚧 Skip remove images"
  fi
}

__buildProjects() {
  if [[ "${buildProjects:-}" == "true" ]]; then
    echo -e "${SEPARATOR} 🔨 Compile and build the image. ${SEPARATOR}"
    cd "${workspace}/${baseProjectPath}"
    mvn -P"${SPRING_PROFILES_ACTIVE}" \
      clean spring-boot:build-image \
      -Dmaven.test.skip=true \
      -Dmaven.build.cache.enabled=false \
      --projects imperative-throughput,reactive-throughput
  else
    echo -e "🚧 Skip build projects"
  fi
}

__loadImages() {
  echo -e "${SEPARATOR} 🐳 A simple way to load images into the kind cluster. ${SEPARATOR}"
  kind load docker-image docker.io/library/imperative-throughput:1.0.0 --name kind-cluster
  kind load docker-image docker.io/library/reactive-throughput:1.0.0 --name kind-cluster
}

__applyWireMock() {
  cd "${workspace}"
  echo -e "${SEPARATOR} 📦 Apply wiremock k8s resources. ${SEPARATOR}"
  kubectl apply -f ./wiremock.yml
  echo -e "${SEPARATOR} ⏳ Wait for wiremock to be ready. ${SEPARATOR}"
  kubectl wait --for=condition=ready pod -l app=wiremock -n wiremock-ns --timeout=2m
}

__applyImperativeThroughput() {
  cd "${workspace}"
  echo -e "${SEPARATOR} 📦 Apply imperative k8s resources. ${SEPARATOR}"
  kubectl apply -f ./imperative-api.yml
  echo -e "${SEPARATOR} ⏳ Wait for the pod to be ready. ${SEPARATOR}"
  kubectl wait --for=condition=ready pod -l app=imperative-api -n imperative-api-ns --timeout=5m
}

__applyReactiveThroughput() {
  cd "${workspace}"
  echo -e "${SEPARATOR} 📦 Apply reactive k8s resources. ${SEPARATOR}"
  kubectl apply -f ./reactive-api.yml
  echo -e "${SEPARATOR} ⏳ Wait for the pod to be ready. ${SEPARATOR}"
  kubectl wait --for=condition=ready pod -l app=reactive-api -n reactive-api-ns --timeout=5m
}

__getResources() {
  echo -e "${SEPARATOR} 📋 Get the resources. ${SEPARATOR}"
  kubectl get all,resourcequotas,ingress -n wiremock-ns -o wide --show-labels
  kubectl get all,resourcequotas,ingress -n imperative-api-ns -o wide --show-labels
  kubectl get all,resourcequotas,ingress -n reactive-api-ns -o wide --show-labels
}

main() {
  __removeImages
  __buildProjects
  __loadImages
  __applyWireMock
  __applyImperativeThroughput
  __applyReactiveThroughput
  __getResources
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main | tee result-apply.log
