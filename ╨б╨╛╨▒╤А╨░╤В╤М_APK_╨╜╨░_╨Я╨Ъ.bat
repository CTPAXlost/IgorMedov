@echo off
chcp 65001 >nul
setlocal
call gradlew.bat clean assembleDebug
if errorlevel 1 (
  echo.
  echo Сборка завершилась с ошибкой. Проверьте Java 17 и Android SDK 35.
  pause
  exit /b 1
)
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "Ремонтник-Старый-дом.apk" >nul
echo.
echo Готово: Ремонтник-Старый-дом.apk
pause
