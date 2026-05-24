#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR/front-electron"

if ! command -v electron >/dev/null 2>&1; then
  printf "Missing 'electron' in PATH.\\n" >&2
  printf "On Arch Linux: sudo pacman -S electron\\n" >&2
  exit 1
fi

electron .
