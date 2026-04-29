#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/housing-platform}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-80}"

echo "[1/5] Entering app directory: ${APP_DIR}"
cd "${APP_DIR}"

echo "[2/5] Building backend"
cd backend
mvn -q -DskipTests package

echo "[3/5] Building frontend"
cd ../frontend
npm ci
npm run build

echo "[4/5] Restarting backend"
cd ..
pkill -f "housing-platform-0.0.1-SNAPSHOT.jar" || true
nohup java -jar backend/target/housing-platform-0.0.1-SNAPSHOT.jar --server.port="${BACKEND_PORT}" > backend.remote.log 2>&1 &

echo "[5/5] Deploying frontend dist"
if command -v nginx >/dev/null 2>&1; then
  sudo mkdir -p /usr/share/nginx/html/housing
  sudo cp -r frontend/dist/* /usr/share/nginx/html/housing/
  sudo nginx -t
  sudo systemctl reload nginx
  echo "Frontend copied to /usr/share/nginx/html/housing"
else
  echo "nginx not found. Frontend dist is ready at ${APP_DIR}/frontend/dist"
fi

echo "Done. Backend port: ${BACKEND_PORT}, frontend port: ${FRONTEND_PORT}"
