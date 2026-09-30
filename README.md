# SmartAccess Android automation

Java 21, TestNG, Maven, and Appium automation for the `Ivis Dev` Flutter APK.

## Stack

- Java 21
- Maven 3.9+
- TestNG
- Appium Java Client with UiAutomator2
- Jackson JSON configuration

## Open in IntelliJ IDEA

1. Open `D:\SmartAccess`.
2. Import the detected `pom.xml` as a Maven project.
3. Select JDK 21 under **File > Project Structure > Project SDK**.
4. Allow Maven to download and index dependencies.

## Run

APK binaries are not stored in Git. Place the required APK under `app/` and set
its relative path in your ignored `config/*.json` file before running the suite.

Run these commands in PowerShell or IntelliJ's PowerShell terminal. Use JDK 21,
enable USB debugging, and accept the phone's USB authorization prompt. Run only
one test session on the device at a time. Access tests can operate the physical
door and ARM/DISARM state; verify the selected JSON before running.

### Check tools and connected devices

```powershell
cd D:\SmartAccess
java -version
mvn -version
adb devices
```

### Run the full suite on the connected phone

```powershell
cd D:\SmartAccess
.\scripts\run-tests.ps1 -Device RZGL20YYSMF -Config config\excecution.json
```

Replace the device serial with the value from `adb devices` if using another phone.
The suite includes app-launch, permission, and configured end-to-end tests.
Once a flow logs out and requests execution stop, remaining tests are skipped.

### Start an emulator instead (optional)

```powershell
.\scripts\start-emulator.ps1 -AvdName Pixel_8a
.\scripts\run-tests.ps1 -Config config\excecution.json
```

Use an installed AVD name. Physical-device FRS and Bluetooth behavior cannot be
assumed to work on an emulator.

The runner checks for a connected device and starts Appium automatically. If
Appium and a device are already ready, Maven can be run directly:

```powershell
mvn test "-DtestConfig=config/excecution.json" "-Ddevice=RZGL20YYSMF"
```

### Start Appium with a fresh log (for direct Maven runs)

Only run this if Appium is not already listening on port 4723. Keep this terminal
open and run Maven in a second terminal.

```powershell
cd D:\SmartAccess
New-Item -ItemType Directory -Force reports | Out-Null
appium --log-timestamp --log reports/appium-live.log
```

### Run individual tests (Appium must already be running)

Full configured login-to-door flow, including conditional Bluetooth handling:

```powershell
mvn test "-Dtest=LoginAndSiteTest" "-DtestConfig=config/excecution.json" "-Ddevice=RZGL20YYSMF"
```

App launch only:

```powershell
mvn test "-Dtest=AppLaunchTest" "-DtestConfig=config/excecution.json" "-Ddevice=RZGL20YYSMF"
```

Permissions only:

```powershell
mvn test "-Dtest=PermissionTest" "-DtestConfig=config/excecution.json" "-Ddevice=RZGL20YYSMF"
```

### Standalone ARM/DISARM check (no door transaction)

Set `execution.armPolicy` in the selected JSON to `disarm-if-armed` for DISARM
or `arm-if-disarmed` for ARM. Then run with Appium already running:

```powershell
mvn test "-Dtest=ArmDisarmTest" "-DtestConfig=config/excecution.json" "-Ddevice=RZGL20YYSMF"
```

Control the standalone test with this block inside `execution`:

```json
"armDisarm": {
  "mode": "arm-disarm-cycle",
  "iterations": 3,
  "logoutAfterCompletion": false
}
```

`arm-disarm-cycle` ensures ARM, then DISARM in each iteration. It skips a toggle
if already in the target state, so an initially armed site skips the first ARM.
`configured-action` instead checks the target from `armPolicy` each iteration;
it does not alternate states. Iterations must be positive; failures stop the loop.
Defaults when omitted: configured-action, one iteration, no logout.

The test logs in once, keeps the same session throughout the loop, and verifies
each target state. Logout runs only after all iterations when enabled in JSON.
After each actual ARM or DISARM operation it returns through Smart Access and selects
the configured site again before verifying its state and continuing. A cycle is
ARM -> Smart Access -> site -> DISARM -> Smart Access -> site. There is no logout
or repeat login between cycles. Enable `logoutAfterCompletion` for final logout.
No logout does not keep the automation session alive: normal teardown still saves
the recording/report and quits the driver; app visibility depends on Appium settings.
FRS requires manual face/blink interaction for each actual state change.
It is excluded from the default suite groups to avoid extra state changes.
Use `disarm-if-armed` when returning to the normal door-access test.

Bluetooth, MCQ, OTP, and logout are page objects, not standalone
test classes, so they do not have separate runnable Maven test commands.
Bluetooth handling runs when its screen appears after FRS in an ARM/DISARM flow;
the default end-to-end test uses `disarm-if-armed`, not a standalone ARM operation.
Follow the on-phone face/blink prompts and enter OTP manually; do not start a
second execution while waiting.

### Compile without launching the application

```powershell
mvn test-compile -DskipTests
```

### Open the latest flow dashboard

```powershell
$latestFlow = Get-ChildItem D:\SmartAccess\reports\flows -Directory |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($latestFlow) {
    Invoke-Item (Join-Path $latestFlow.FullName 'index.html')
}
```

TestNG reports are generated under `reports/testng`. Failed tests save screenshots
and Android UI XML under `screenshots`.

Each device session also produces `reports/flows/<timestamp>-<test-class>/index.html`.
This evidence report includes a screenshot after every automated click (including
input focus), named checkpoints, test results, and playable MP4 recordings. Recording
starts after Appium launches the app and stops before the driver closes. Longer runs
are split into recording parts, with brief gaps possible between parts. Manual phone
actions appear in the recording. Automated clicks and explicit checkpoints create
screenshots, including populated OTP fields, OTP acceptance, and the Diagnostics
screen when detected during verification or opening.
Share the entire session folder to preserve all image and video attachments.
Capture errors are shown in the report. Reports may contain visible credentials,
OTP values, or faces, and remain in the Git-ignored `reports` directory.
The CRM-style dashboard provides status/count cards, a searchable screenshot gallery,
an activity timeline, full-size screenshot previews, and a recordings tab. Its offline
template is maintained in `src/test/resources/report-dashboard.html`.

Every run follows the `execution` section of the selected JSON file. The flow
applies the URL, account ID and login, opens Smart Access, selects the configured
site, and disarms it when necessary. It then re-enters Smart Access, selects the
configured door and reason, completes FRS and security questions, and opens or
cancels the door request. When an OTP screen appears, enter the four-digit OTP
manually on the connected phone within five minutes. Automation then clicks Verify.
If the app skips OTP, the flow proceeds directly to the door confirmation.
After a successful Vault opening, automation clicks OK, opens Profile, logs out,
verifies logout, saves the report/recording, and closes the app and driver.

## Page objects

Each screen has a separate page class under `src/test/java/com/smartaccess/automation/pages`:

- `BaseUrlPage`, `AccountPage`, `LoginPage`: environment and login.
- `DashboardPage`, `SitePage`: Smart Access and site selection.
- `ArmDisarmPage`: ARM/DISARM status and confirmation.
- `BluetoothArmDisarmPage`: offline Bluetooth sending state, received ARMED/DISARMED
  status and matching site-success message, then Done; captures observed states.
- `DiagnosticsPage`: captures the asynchronous Diagnostics connection screen.
- `DoorSelectionPage`: door, access reason, confirmation, and optional warning.
- `FrsPage`: face capture review and authentication results, shared by both flows.
- `SecurityQuestionsPage`: configured MCQ answers and the next-screen check.
- `OtpPage`: manual phone entry, verification, and invalid-OTP retry.
- `DoorConfirmationPage`: Open/Cancel and the operation result.
- `ProfilePage`, `ProfileRecoveryPage`: profile/logout and revoked-profile recovery.
- `PanicPage`: panic confirmation.
- `AlertPage`: detects "Please close <door name> ... Retry" for any door,
  clicks OK, verifies the return to the sites list, then opens Profile and logs out
  (including the logout confirmation when present). The blocked execution
  is marked skipped with the alert message; close the physical door before rerunning.

`LoginAndSiteTest` coordinates the full execution. `VaultAccessPage` delegates the
door-access sequence to the individual page objects. The default configuration is
`config/excecution.json` (filename spelling retained as requested).

Example execution structure (use real answers only in the ignored local JSON):

```json
"execution": {
  "siteId": "SITE_ID",
  "door": "Vault",
  "accessReason": "Customer",
  "armPolicy": "disarm-if-armed",
  "securityAnswers": {
    "Access Codes?": "<answer>",
    "Date of birth?": "<answer>",
    "Date of joining?": "<answer>"
  },
  "otpMode": "manual",
  "doorAction": "open",
  "profileUserName": "<profile name>",
  "approvedAction": "close"
}
```

If FRS reports that the face is not recognized, the flow opens the profile,
verifies Revoked status, captures and submits a replacement profile image, then
reopens the profile. An Approved profile is closed (or logged out when configured);
an unapproved profile is logged out. Appium automatically grants runtime permissions.

The real JSON configuration is ignored by Git because it contains credentials.
