#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BACK_DIR="$ROOT_DIR/back"
TARGET_DIR="$BACK_DIR/target"
FRONT_DIR="$ROOT_DIR/front-electron"
JAR_PATH="$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'ferrum-*.jar' ! -name '*.original' | head -n 1 || true)"

backend_needs_rebuild() {
  if [[ -z "${JAR_PATH}" || ! -f "${JAR_PATH}" ]]; then
    return 0
  fi

  if [[ "$BACK_DIR/pom.xml" -nt "$JAR_PATH" ]]; then
    return 0
  fi

  if find "$BACK_DIR/src" -type f -newer "$JAR_PATH" | read -r _; then
    return 0
  fi

  return 1
}

if backend_needs_rebuild; then
  printf "Rebuilding backend JAR...\\n"
  mvn -q -f "$BACK_DIR/pom.xml" -Dmaven.repo.local=/tmp/.m2 -DskipTests package
fi

cd "$FRONT_DIR"

if ! command -v electron >/dev/null 2>&1; then
  printf "Missing 'electron' in PATH.\\n" >&2
  printf "On Arch Linux: sudo pacman -S electron\\n" >&2
  exit 1
fi

electron .
