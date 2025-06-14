#!/usr/bin/env bash

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

__createCluster() {
  echo -e "${SEPARATOR} 🚢 create a k8s cluster. ${SEPARATOR}"
  # https://kind.sigs.k8s.io/
  # https://kind.sigs.k8s.io/docs/user/quick-start/#creating-a-cluster
  kind create cluster --config kind-cluster-config.yml
  kind get clusters
  kubectl cluster-info --context kind-kind-cluster
}

__installIngressController() {
  echo -e "${SEPARATOR} 🌐 configure ingress controller (NGINX). ${SEPARATOR}"
  # https://kind.sigs.k8s.io/docs/user/ingress/
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s
}

__installMetricsServer() {
  echo -e "${SEPARATOR} 📊 install metric server. ${SEPARATOR}"
  # https://github.com/kubernetes-sigs/metrics-server/
  # full list of Metrics Server configuration flags: docker run --rm registry.k8s.io/metrics-server/metrics-server:v0.6.0 --help
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  kubectl patch -n kube-system deployment metrics-server --type=json \
    -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
  kubectl top nodes
}

main() {
  __createCluster
  __installIngressController
  __installMetricsServer
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main
