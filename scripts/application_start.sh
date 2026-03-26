#!/bin/bash
set -euxo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo ".env 파일이 없습니다."
  exit 1
fi

APP_IMAGE="$(grep '^APP_IMAGE=' .env | cut -d= -f2-)"
SERVICE_PORT="$(grep '^SERVICE_PORT=' .env | cut -d= -f2-)"

if [ -z "$APP_IMAGE" ]; then
  echo "APP_IMAGE 값이 없습니다."
  exit 1
fi

if [ -z "$SERVICE_PORT" ]; then
  echo "SERVICE_PORT 값이 없습니다."
  exit 1
fi

mkdir -p /opt/be-app/logs

if ! docker network inspect app-net >/dev/null 2>&1; then
  echo "Docker network 'app-net' 이 존재하지 않습니다."
  exit 1
fi

docker rm -f be-app || true

docker run -d \
  --name be-app \
  --network app-net \
  -p "${SERVICE_PORT}:${SERVICE_PORT}" \
  -e SERVER_PORT="${SERVICE_PORT}" \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file .env \
  -v /opt/be-app/logs:/app/logs \
  --restart unless-stopped \
  "$APP_IMAGE"

sleep 10
docker ps | grep be-app