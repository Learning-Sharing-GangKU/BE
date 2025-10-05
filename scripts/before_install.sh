#!/bin/bash
set -euxo pipefail

# 배포 디렉토리 생성
sudo mkdir -p /opt/be-app
sudo chown -R ubuntu:ubuntu /opt/be-app
