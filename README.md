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

```powershell
.\scripts\start-emulator.ps1 -AvdName Pixel_8a
.\scripts\run-tests.ps1 -Config config\uat1.json
```

The runner checks for a connected device and starts Appium automatically. If
Appium and a device are already ready, Maven can be run directly:

```powershell
mvn test -DtestConfig=config\uat1.json
```

TestNG reports are generated under `reports/testng`. Failed tests save screenshots
and Android UI XML under `screenshots`.

The end-to-end Page Object flow applies the base URL, enters the account ID and
credentials, opens Smart Access, selects the MAFIN site, taps Arm/Disarm, and
performs FRS with one face-recognition retry, verifies success, and handles the
ARM confirmation. The PowerShell runner accepts `-ArmModeAction activate`,
`deactivate`, or `cancel-deactivation`; `activate` is the default. Appium automatically
grants the APK's runtime permissions during installation. Its locators are in
`src/test/java/com/smartaccess/automation/pages`.

The real JSON configuration is ignored by Git because it contains credentials.
