#!/bin/bash
set -euxo pipefail
cd "$(dirname "$0")/.."

APP_IMAGE=$(grep '^APP_IMAGE=' .env | cut -d= -f2)
AWS_REGION=$(grep '^AWS_REGION=' .env | cut -d= -f2)

ECR_REGISTRY=$(echo $APP_IMAGE | cut -d'/' -f1)

aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

sudo docker pull $APP_IMAGE