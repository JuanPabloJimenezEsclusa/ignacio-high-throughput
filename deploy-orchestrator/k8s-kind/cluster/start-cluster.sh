#!/usr/bin/env bash

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

__retry() {
  local attempts="${1}"
  local delay="${2}"
  local command="${@:3}"
  local i=0

  until [ $i -ge $attempts ]
  do
    echo "Attempt $((i+1))/$attempts: $command"
    $command && return 0 || {
      echo "Command failed, retrying in $delay seconds..."
      sleep $delay
      i=$((i+1))
    }
  done

  echo "Command '$command' failed after $attempts attempts"
  return 1
}

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
    --timeout=150s
}

__installMetricsServer() {
  echo -e "${SEPARATOR} 📊 install metric server. ${SEPARATOR}"
  # https://github.com/kubernetes-sigs/metrics-server/
  # full list of Metrics Server configuration flags: docker run --rm registry.k8s.io/metrics-server/metrics-server:v0.6.0 --help
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  kubectl patch -n kube-system deployment metrics-server --type=json \
    -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

  # Wait for metrics server to be ready with retries
  if ! __retry 5 10 kubectl top nodes; then
    echo "Warning: Unable to verify metrics-server functionality, but continuing anyway"
  fi
}

__installMonitoring() {
  echo -e "${SEPARATOR} 📊 install monitoring. ${SEPARATOR}"
  helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
  helm repo add stable https://charts.helm.sh/stable
  helm repo update

  kubectl apply -f ./monitoring-ns.yml
  helm install kind-prometheus prometheus-community/kube-prometheus-stack \
    --namespace monitoring-ns \
    --set prometheus.service.nodePort=30000 \
    --set prometheus.service.type=NodePort \
    --set grafana.service.nodePort=31000 \
    --set grafana.service.type=NodePort \
    --set alertmanager.service.nodePort=32000 \
    --set alertmanager.service.type=NodePort \
    --set prometheus-node-exporter.service.nodePort=32001 \
    --set prometheus-node-exporter.service.type=NodePort
  kubectl apply -f ./monitoring-prometheus.yml
}

main() {
  __createCluster
  __installIngressController
  __installMetricsServer
  __installMonitoring
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main
