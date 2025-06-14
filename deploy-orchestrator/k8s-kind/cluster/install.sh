#!/usr/bin/env bash

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${debug:-}" == "true" ]]; then set -o xtrace; fi  # enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

__installKubectl() {
  echo -e "${SEPARATOR} 📦 Install kubectl. ${SEPARATOR}"
  # https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/#install-kubectl-binary-with-curl-on-linux
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
  echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
  sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
  rm kubectl kubectl.sha256
  kubectl version --client --output=yaml
}

__installHelm() {
  echo -e "${SEPARATOR} 📦 Install helm. ${SEPARATOR}"
  # https://helm.sh/docs/intro/install/#from-script
  curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  helm version
  helm repo list || true
}

__installKind() {
  echo -e "${SEPARATOR} 📦 Install kind. ${SEPARATOR}"
  # https://kind.sigs.k8s.io/
  # https://kind.sigs.k8s.io/docs/user/quick-start/#installing-from-release-binaries
  # For AMD64 / x86_64
  [ $(uname -m) = x86_64 ] && curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-amd64
  # For ARM64
  [ $(uname -m) = aarch64 ] && curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-arm64
  chmod +x ./kind
  sudo mv ./kind /usr/local/bin/kind
  kind version
}

main() {
  __installKubectl
  __installHelm
  __installKind
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main

# add in ~/.bashrc
##################

# Load kube auto completion
#source <(kubectl completion bash)

# Load helm completion
#source <(helm completion bash)

# alias
#alias k=kubectl
#complete -o default -F __start_kubectl k
