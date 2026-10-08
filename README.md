# Pocket Ledger — Android SMS expense tracker

Bank-independent INR transaction SMS tracker. Recognises common sender aliases across Indian banks and imports unfamiliar account/card transaction alerts for review. Historical inbox import plus new-SMS capture. All application assets are bundled; the app has no Internet permission and sends no SMS data to a server.

## Delivery status

The Android APK is built by GitHub Actions, with parser/dashboard checks, Android SDK compilation and APK signature verification. The workflow also installs the APK on Android 10 and checks the actual WebView dashboard, charts and navigation. See the latest workflow result and its APK artifact in the repository's Actions tab. No genuine SMS data is included; bank-specific formats still need checking on your own phone.

## Build on Windows with Android Studio

1. Install Android Studio from Google's official site; install its Android SDK Platform 35 and Build Tools 35.0.0.
2. Extract this ZIP and open the `pocket-ledger` folder (the folder containing `settings.gradle`).
3. Use a compatible Gradle JDK (17 or newer supported by Android Gradle Plugin 8.7). In Gradle settings choose a local Gradle 8.9 distribution. This source project does not bundle a Gradle wrapper or distribution.
4. Download Gradle 8.9 from the official Gradle distribution site if needed. With Gradle available on PATH, run `gradle :app:assembleDebug` from this folder. Android Studio may create `local.properties` with your SDK path automatically; otherwise set `ANDROID_HOME` to the Android SDK directory.
5. The resulting file is `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to your Android phone and install it, granting the installation source permission when Android prompts.
6. If you maintain your own GitHub repository, the included manual GitHub Actions workflow is another build option. The included workflow builds the APK. Repository root must contain `settings.gradle`.

## First run

1. Open Pocket Ledger → Settings → Grant SMS access.
2. Allow Read SMS and Receive SMS, then tap Scan all existing messages. The scan covers stored incoming SMS, looking for explicit INR transaction amounts and bank/account/card context. All detected banks appear in the bank filter.
3. Review uncertain entries in Activity → Needs review. Confirm the direction, merchant and category, or exclude failed/duplicate entries.
4. In Overview choose a month and bank. Switch between daily and cumulative spending. Tap the line chart for that day's totals.
5. Set category budgets. Edit a merchant category and tick Remember this merchant category to apply it to matching merchants already imported and future entries.
6. Use Plans to enter a verified available balance, expected income, other costs, a horizon and unpaid future expenses. Negative results are shown as a shortfall. Past-due plans are shown for review and excluded from the future-only calculation; update their date if they remain outstanding.

Permissions can be denied or restricted by Android or the phone's installation policy. Check app permissions in Android Settings. The app does not need to be the default SMS app. Play Store distribution of apps requesting SMS permissions has additional eligibility/review requirements; this project has not been submitted to Google Play. Use only your own genuine SMS messages.

## What is implemented

- Historical inbox scan; incoming multipart-SMS receiver.
- Bank/source, sender, original alert, received date, amount stored as integer paise, direction, account suffix, reference and merchant.
- Exact repeated source identifiers ignored; probable repeated bank alerts retained as review entries instead of silently discarded.
- Explicit failure/future/ambiguous amount language goes to review.
- Failed/review/excluded entries omitted from charts and totals. Confirmed Transfer entries excluded from income and expense totals.
- Dynamic bank/month filters, search, direction/status filters, transaction editor including editable bank name.
- Market-style spending line chart (daily/cumulative), category doughnut, six-month income/expense bars, biggest spend and category share insights.
- Monthly category budget progress; repeated category limits.
- Future-expense horizon with shortfall calculator.
- CSV export through Android's document picker, with CSV formula-escape protection.
- Delete tracker data without deleting SMS; automatic app backup disabled.
- Refreshes an open dashboard every five seconds for newly imported messages.

## Accuracy and privacy limits

This is **SMS-based tracking**, not Account Aggregator or direct bank sync. It only sees messages present/received on this Android phone. No alert means no automatic entry. Balance-only alerts, OTPs, unrelated messages and unsupported formats are not transactions. Promotional messages and unexpected bank wording can still need correction.

The alert timestamp is used; it is not always the payment timestamp. Sender labels and message contents do not establish authenticity. Rules match the extracted merchant name exactly, ignoring case. An Unknown merchant cannot be learned because that would incorrectly assign all unknown payees the same category.

Account suffix can be Unknown if not present. Charts aggregate by bank; bank names can be corrected in the transaction editor. Exact individual account selection is not implemented. India's Post Office Savings accounts and IPPB are both grouped as India Post based on sender labels.

Net cash flow is income minus expenses from imported alerts, **not your bank balance or guaranteed savings**. Refunds appear as credits and are not linked to the original expense. Future plans are entered manually and are not auto-settled when a payment arrives. Remove or update them yourself to avoid double counting in forecasts. No recurring-expense detector or statement-import parser is included in this version.

Data lives in Android's private app storage (SQLite), not in a separately encrypted database. There is no custom biometric app lock. CSV files you export may contain sensitive merchant/reference information and are stored wherever you choose. Clearing all data removes rules, budgets and plans. Uninstalling also removes app data.

## Validation

From this project folder:

- `node --check app/src/main/assets/app.js`
- `node tests/ui-test.cjs` — verifies actual dashboard logic with a minimal DOM adapter: totals, bank filters, review filtering, future shortfall/horizon and empty-state behavior. This is not a browser rendering test.
- `javac -d /tmp/pocket-tests app/src/main/java/in/pocketledger/app/SmsParser.java tests/ParserTest.java`
- `java -cp /tmp/pocket-tests ParserTest` — Parser checks against synthetic bank message fixtures, not the user's actual SMS.

If `javac` is missing but Java includes the compiler module, use `java -m jdk.compiler/com.sun.tools.javac.Main` in place of `javac`.

Phone acceptance checks: scan representative genuine alerts from all three banks; compare a monthly total to your statements; repeat scan and inspect duplicates; send/receive a genuine bank alert after a real transaction; relaunch the app; test denied permissions, category learning, transfers, review confirmation, export and deletion. Never add fabricated financial messages to test real lender systems.

## UI preview

Open `PREVIEW.html` in a modern browser to inspect the dashboard layout. It intentionally starts empty. SMS permissions and scans only work in a built Android app. There is no simulated bank connection.

## Broad bank detection (1.1)

The scanner is no longer restricted to three banks. It identifies common sender aliases for SBI, ICICI, Axis, Kotak, Canara, PNB, Union Bank, Bank of Baroda, Indian Bank, IOB, IDFC FIRST, Federal, Yes Bank, IndusInd, IDBI and many others, while retaining KVB/HDFC/India Post. This alias registry is heuristic, not an official verified sender directory. The sender label takes precedence over names mentioned in a counterparty or payee.

Unfamiliar sources with explicit INR transactions and account/card context are retained as review entries, labelled from a bank name in the text or Other bank plus sender. They remain outside totals until verified. Edit Bank and confirm the entry after checking it. Personal phone-number messages, OTPs, balance-only alerts and due-date reminders are excluded. No parser can guarantee every possible bank's wording; inspect review entries and compare totals with statements. Re-scan history after upgrading to capture previously unsupported messages.

### Automatic category tagging
Merchant/place names in each SMS are matched against conservative offline rules for Food, Travel, Groceries, Shopping, Health, Bills and Entertainment. Unclear names stay **Untagged**; personal names and generic UPI IDs cannot reveal what was purchased. In Transactions, choose Untagged or Tagged. Saving a category immediately moves that entry into Tagged. Optionally remember the merchant to tag matching existing and future transactions. Bank verification status stays separate from category tagging.
