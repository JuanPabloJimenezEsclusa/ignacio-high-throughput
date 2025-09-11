#!/usr/bin/env bash

# Example of usage: ./init-aws-stack.sh buildProjects=true

set -o errexit # Exit on error. Append "|| true" if you expect an error.
set -o errtrace # Exit on error inside any functions or subshells.
set -o nounset # Do not allow use of undefined vars. Use ${VAR:-} to use an undefined VAR
if [[ "${DEBUG:-}" == "true" ]]; then set -o xtrace; fi  # Enable debug mode.

SEPARATOR="\n ################################################## \n"

cd "$(dirname "$0")"

parameter="${1:-"buildProjects=false"}"
eval "${parameter}"
echo "buildProjects: ${buildProjects:-}"

workspace="$(pwd)"
baseProjectPath="../../"

# Environment variables
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-"native"}"

PUBLIC_REGISTRY="${PUBLIC_REGISTRY:-public.ecr.aws}"
ECR_PUBLIC_ALIAS="${ECR_PUBLIC_ALIAS:-q9k9a5q7}"
ECR_REPOSITORY_IMPERATIVE_NAME="${ECR_REPOSITORY_IMPERATIVE_NAME:-imperative-throughput}"
ECR_REPOSITORY_REACTIVE_NAME="${ECR_REPOSITORY_REACTIVE_NAME:-reactive-throughput}"
ECR_IMAGE_TAG="${ECR_IMAGE_TAG:-1.0.0}"

__install_aws_cli() {
  echo -e "${SEPARATOR} 🛠️ Install aws cli. (${FUNCNAME:-}) ${SEPARATOR}"
  curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  unzip awscliv2.zip
  sudo ./aws/install --update
  rm -rf awscliv2.zip aws
  aws --version
  echo "End ${FUNCNAME:-} successfully!"
}

__install_k9s() {
  echo -e "${SEPARATOR} 🛠️ Install k9s. (${FUNCNAME:-}) ${SEPARATOR}"
  wget https://github.com/derailed/k9s/releases/latest/download/k9s_linux_amd64.deb \
    && sudo apt install ./k9s_linux_amd64.deb \
    && rm k9s_linux_amd64.deb || true
  k9s version
  echo "End ${FUNCNAME:-} successfully!"
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

__create_ecr_repository() {
  echo -e "${SEPARATOR} 📦 Create ECR repositories if it doesn't exist. ${SEPARATOR}"
  aws ecr-public describe-repositories --repository-names "${ECR_REPOSITORY_IMPERATIVE_NAME}" --region us-east-1 >/dev/null 2>&1 || \
    aws ecr-public create-repository --repository-name "${ECR_REPOSITORY_IMPERATIVE_NAME}" --region us-east-1
  aws ecr-public describe-repositories --repository-names "${ECR_REPOSITORY_REACTIVE_NAME}" --region us-east-1 >/dev/null 2>&1 || \
    aws ecr-public create-repository --repository-name "${ECR_REPOSITORY_REACTIVE_NAME}" --region us-east-1
}

__login_to_ecr() {
  echo -e "${SEPARATOR} 🔑 Login to ECR. ${SEPARATOR}"
  # Public ECR login always uses us-east-1
  aws ecr-public get-login-password --region us-east-1 | \
    docker login --username AWS --password-stdin "${PUBLIC_REGISTRY}"
}

__build_and_push_image() {
  echo -e "${SEPARATOR} 🐳 Build and push Docker images to public ECR. ${SEPARATOR}"
  docker tag "${ECR_REPOSITORY_IMPERATIVE_NAME}:${ECR_IMAGE_TAG}" "${PUBLIC_REGISTRY}/${ECR_PUBLIC_ALIAS}/${ECR_REPOSITORY_IMPERATIVE_NAME}:${ECR_IMAGE_TAG}"
  docker tag "${ECR_REPOSITORY_REACTIVE_NAME}:${ECR_IMAGE_TAG}" "${PUBLIC_REGISTRY}/${ECR_PUBLIC_ALIAS}/${ECR_REPOSITORY_REACTIVE_NAME}:${ECR_IMAGE_TAG}"
  docker push "${PUBLIC_REGISTRY}/${ECR_PUBLIC_ALIAS}/${ECR_REPOSITORY_IMPERATIVE_NAME}:${ECR_IMAGE_TAG}"
  docker push "${PUBLIC_REGISTRY}/${ECR_PUBLIC_ALIAS}/${ECR_REPOSITORY_REACTIVE_NAME}:${ECR_IMAGE_TAG}"
}

__create_ecs_stack() {
  echo -e "${SEPARATOR} ☁️ Create ecs stack. ${SEPARATOR}"
  cd "${workspace}"
  aws cloudformation create-stack \
    --stack-name "high-throughput-services-stack" \
    --template-body "file://high-throughput-services-stack.yml" \
    --capabilities CAPABILITY_NAMED_IAM

  # Wait for stack to be created
  echo "Waiting ${FUNCNAME:-} ..."
  aws cloudformation wait stack-create-complete \
    --stack-name "high-throughput-services-stack"

  echo "End ${FUNCNAME:-} successfully!"
}

# Main script
main() {
  echo "Init ${0##*/} (${FUNCNAME:-})"
  __install_k9s
  __install_aws_cli
  __buildProjects
  __create_ecr_repository
  __login_to_ecr
  __build_and_push_image
  __create_ecs_stack
  echo "Done ${0##*/} (${FUNCNAME:-})"
}

echo -e "${SEPARATOR} 🔨 Main: ${0} ${SEPARATOR}"
time main | tee result-start.log
