#!/bin/bash
set -euxo pipefail
cd "$(dirname "$0")/.."

# .env 에서 변수 읽기
APP_IMAGE="$(grep '^APP_IMAGE=' .env | cut -d= -f2)"
AWS_REGION="$(grep '^AWS_REGION=' .env | cut -d= -f2)"

# ECR 로그인
aws ecr get-login-password --region "$AWS_REGION" \
 | docker login --username AWS --password-stdin "$(echo "$APP_IMAGE" | awk -F: '{print $1}')"

# 최신 이미지 풀
sudo docker pull "$APP_IMAGE"
