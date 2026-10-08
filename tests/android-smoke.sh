#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-output
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n in.pocketledger.app/.MainActivity | tee smoke-output/launch.txt
sleep 8
adb shell pidof in.pocketledger.app | tee smoke-output/pid.txt
adb exec-out screencap -p > smoke-output/dashboard.png
adb logcat -d > smoke-output/logcat.txt
if rg -q 'FATAL EXCEPTION|Unable to start activity|Process: in.pocketledger.app.*FATAL' smoke-output/logcat.txt; then
  echo 'Android launch crash detected'; exit 1
fi
if rg -qi 'Uncaught (ReferenceError|TypeError|SyntaxError)|Refused to (load|execute).*script' smoke-output/logcat.txt; then
  echo 'Web dashboard initialization error detected'; exit 1
fi
