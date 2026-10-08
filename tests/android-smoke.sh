#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-output
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n in.pocketledger.app/.MainActivity | tee smoke-output/launch.txt
sleep 4
adb shell uiautomator dump /sdcard/permission.xml
adb pull /sdcard/permission.xml smoke-output/permission.xml
python3 - <<'PYTEST'
from pathlib import Path
s=Path('smoke-output/permission.xml').read_text()
assert 'permissioncontroller' in s and ('SMS' in s or 'sms' in s), 'First-launch SMS permission prompt must be visible'
print('First-launch SMS permission prompt verified.')
PYTEST
adb exec-out screencap -p > smoke-output/permission-prompt.png
adb shell pm grant in.pocketledger.app android.permission.READ_SMS
adb shell pm grant in.pocketledger.app android.permission.RECEIVE_SMS
adb shell am force-stop in.pocketledger.app
adb shell am start -W -n in.pocketledger.app/.MainActivity
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

adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w in.pocketledger.app.test/in.pocketledger.app.LedgerInstrumentation | tee smoke-output/native-db-tests.txt
python3 - <<'PYTEST'
from pathlib import Path
text=Path('smoke-output/native-db-tests.txt').read_text()
assert 'PASS isolated native DB' in text and 'FAIL' not in text, text
print('Native database regression tests passed in isolated test storage.')
PYTEST
