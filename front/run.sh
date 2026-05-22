#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACK_DIR="$ROOT_DIR/back"
TARGET_DIR="$BACK_DIR/target"
BACKEND_PORT="${FERRUM_BACKEND_PORT:-18080}"
BACKEND_URL="${FERRUM_BACKEND_URL:-http://localhost:${BACKEND_PORT}}"
BACKEND_LOG="${FERRUM_BACKEND_LOG:-/tmp/ferrum-backend.log}"
BACKEND_PID=""

cleanup() {
  if [[ -n "${BACKEND_PID}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
    kill "${BACKEND_PID}" 2>/dev/null || true
    wait "${BACKEND_PID}" 2>/dev/null || true
  fi
}

trap cleanup EXIT

JAR_PATH="$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'ferrum-*.jar' ! -name '*.original' | head -n 1 || true)"

if [[ -z "${JAR_PATH}" ]]; then
  mvn -q -f "$BACK_DIR/pom.xml" -DskipTests package
  JAR_PATH="$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'ferrum-*.jar' ! -name '*.original' | head -n 1)"
fi

export PYTHONPATH="$ROOT_DIR/front${PYTHONPATH:+:$PYTHONPATH}"
export FERRUM_BACKEND_URL="$BACKEND_URL"

java -jar "$JAR_PATH" --server.port="$BACKEND_PORT" >"$BACKEND_LOG" 2>&1 &
BACKEND_PID="$!"

python3 -m ferrum_gnome.app
