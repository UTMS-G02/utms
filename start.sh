#!/bin/bash

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)

echo "==> Database başlatılıyor..."
docker-compose -f "$ROOT_DIR/docker-compose.yml" up -d

echo "==> Database hazır olana kadar bekleniyor..."
until docker exec utms-postgres pg_isready -U utms_user -d utms_db > /dev/null 2>&1; do
  sleep 1
done
echo "    Database hazır."

echo "==> Backend derleniyor..."
cd "$ROOT_DIR/backend"
./mvnw package -DskipTests -q
echo "==> Backend başlatılıyor..."
java -jar target/utms-app-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "    Backend PID: $BACKEND_PID"

echo "==> Frontend başlatılıyor..."
cd "$ROOT_DIR/frontend"
npm run dev > "$ROOT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "    Frontend PID: $FRONTEND_PID"

echo ""
echo "Tüm servisler başlatıldı."
echo "  Database  -> localhost:5432"
echo "  Backend   -> http://localhost:8080"
echo "  Frontend  -> http://localhost:5173"
echo ""
echo "Loglar: backend.log / frontend.log"
echo "Durdurmak için: ./stop.sh"

wait