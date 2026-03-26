#!/bin/bash
set -euxo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo ".env 파일이 없습니다."
  exit 1
fi

APP_IMAGE="$(grep '^APP_IMAGE=' .env | cut -d= -f2-)"
AWS_REGION="$(grep '^AWS_REGION=' .env | cut -d= -f2-)"

if [ -z "$APP_IMAGE" ]; then
  echo "APP_IMAGE 값이 없습니다."
  exit 1
fi

if [ -z "$AWS_REGION" ]; then
  echo "AWS_REGION 값이 없습니다."
  exit 1
fi

ECR_REGISTRY="$(echo "$APP_IMAGE" | cut -d/ -f1)"

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

docker pull "$APP_IMAGE"