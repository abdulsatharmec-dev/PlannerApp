#!/usr/bin/env bash
# Install the DayRoute debug APK via adb (USB or wireless).
# Uses ANDROID_HOME platform-tools adb only (avoids mixing with /usr/bin/adb).
#
# Usage:
#   ./scripts/install-debug-adb.sh              # install if exactly one usable device
#   ./scripts/install-debug-adb.sh devices    # adb devices -l
#   ./scripts/install-debug-adb.sh pair IP:PAIR_PORT
#   ./scripts/install-debug-adb.sh connect IP:CONNECT_PORT
#
# Wireless (first time): on the phone open Developer options → Wireless debugging,
# run "pair" with the pairing IP:port and Wi‑Fi pairing code, then "connect" with
# the connection IP:port shown there.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

if [[ ! -x "$ADB" ]]; then
  echo "adb not found at $ADB" >&2
  echo "Set ANDROID_HOME to your Android SDK (or install SDK platform-tools)." >&2
  exit 1
fi

export PATH="$ANDROID_HOME/platform-tools:$PATH"

cmd="${1:-install}"
shift || true

case "$cmd" in
  devices)
    "$ADB" devices -l
    ;;
  pair)
    if [[ $# -lt 1 ]]; then
      echo "Usage: $0 pair IP:PAIR_PORT" >&2
      exit 1
    fi
    "$ADB" pair "$@"
    ;;
  connect)
    if [[ $# -lt 1 ]]; then
      echo "Usage: $0 connect IP:CONNECT_PORT" >&2
      exit 1
    fi
    "$ADB" connect "$@"
    "$ADB" devices -l
    ;;
  install)
    mapfile -t devs < <("$ADB" devices | awk -F'\t' '/\tdevice$/ {print $1}')
    n="${#devs[@]}"
    if [[ "$n" -eq 0 ]]; then
      echo "No device in 'adb devices' (need status 'device')." >&2
      echo "USB: enable USB debugging and accept the key." >&2
      echo "Wireless: $0 pair IP:PAIR_PORT  then  $0 connect IP:PORT" >&2
      exit 1
    fi
    if [[ "$n" -gt 1 ]]; then
      echo "Multiple devices connected; disconnect extras or use -s:" >&2
      printf '  %s\n' "${devs[@]}" >&2
      exit 1
    fi
    cd "$ROOT"
    ./gradlew :app:installDebug
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    echo "Use: install | devices | pair IP:PORT | connect IP:PORT" >&2
    exit 1
    ;;
esac
