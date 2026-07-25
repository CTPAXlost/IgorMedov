@echo off
chcp 65001 >nul
setlocal
set "ROOT=%~dp0"
set "ANDROID_PROJECT=%ROOT%android"

if not defined JAVA_HOME (
  where java >nul 2>nul || (
    echo Не найдена Java. Установите Android Studio или JDK 17.
    pause
    exit /b 1
  )
)

if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if defined ANDROID_HOME (
  set "SDK_ESC=%ANDROID_HOME:\=/%"
  >"%ANDROID_PROJECT%\local.properties" echo sdk.dir=%SDK_ESC%
)

cd /d "%ANDROID_PROJECT%"
echo.
echo Сборка APK. При первом запуске Gradle и зависимости загрузятся автоматически.
echo.
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Сборка не завершена. Откройте папку android в Android Studio,
  echo дождитесь установки Android SDK 35 и повторите сборку.
  pause
  exit /b 1
)

set "APK=%ANDROID_PROJECT%\app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK%" (
  copy /y "%APK%" "%ROOT%Ремонтник_3_уровня.apk" >nul
  echo.
  echo Готово: %ROOT%Ремонтник_3_уровня.apk
) else (
  echo APK не найден после сборки.
)
pause
