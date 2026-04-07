#!/bin/bash
set -euxo pipefail

APP_DIR="/opt/be-app"
LOG_DIR="$APP_DIR/logs"

mkdir -p "$APP_DIR"
mkdir -p "$LOG_DIR"

chown ubuntu:ubuntu "$APP_DIR"
chown ubuntu:ubuntu "$LOG_DIR"
chmod 777 "$APP_DIR"
chmod 777 "$LOG_DIR"