#!/bin/bash

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)

echo "==> Backend durduruluyor..."
pkill -f "spring-boot:run" 2>/dev/null
pkill -f "utms_app" 2>/dev/null

echo "==> Frontend durduruluyor..."
pkill -f "vite" 2>/dev/null

echo "==> Database durduruluyor..."
docker-compose -f "$ROOT_DIR/docker-compose.yml" stop

echo "Tüm servisler durduruldu."