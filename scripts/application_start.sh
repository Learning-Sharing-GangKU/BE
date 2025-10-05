#!/bin/bash
set -euxo pipefail
cd "$(dirname "$0")/.."

APP_IMAGE="$(grep '^APP_IMAGE=' .env | cut -d= -f2)"
SERVICE_PORT="$(grep '^SERVICE_PORT=' .env | cut -d= -f2)"

# 기존 컨테이너 정리
sudo docker rm -f be-app || true

# 컨테이너 실행
sudo docker run -d \
  --name be-app \
  -p "${SERVICE_PORT}:${SERVICE_PORT}" \
  -e SERVER_PORT="${SERVICE_PORT}" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -v /opt/be-app/logs:/app/logs \
  --restart unless-stopped \
  "$APP_IMAGE"

# 기동 여유
sleep 10
