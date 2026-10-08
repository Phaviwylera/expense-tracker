#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-output
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n in.pocketledger.app/.MainActivity | tee smoke-output/launch.txt
sleep 8
app_pid=$(adb shell pidof in.pocketledger.app | tr -d '\r')
echo "$app_pid" | tee smoke-output/pid.txt
adb forward tcp:9222 "localabstract:webview_devtools_remote_$app_pid"
trap 'adb exec-out screencap -p > smoke-output/dashboard.png; adb logcat -d > smoke-output/logcat.txt' EXIT
node tests/browser-smoke.cjs
adb exec-out screencap -p > smoke-output/dashboard.png
adb logcat -d > smoke-output/logcat.txt
python3 - <<'PY'
from pathlib import Path
import re
s=Path('smoke-output/logcat.txt').read_text(errors='replace')
errors=re.findall(r'.*(?:FATAL EXCEPTION|Unable to start activity|Uncaught (?:ReferenceError|TypeError|SyntaxError)|Refused to (?:load|execute).*script).*',s)
if errors:
 print('\n'.join(errors));raise SystemExit('Android or dashboard initialization failed')
print('No Android crashes or JavaScript initialization errors.')
PY
