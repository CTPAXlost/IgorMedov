@echo off
chcp 65001 >nul
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo На компьютере не найден Gradle.
  echo Рекомендуемый способ: загрузите проект на GitHub и запустите Actions - Сборка APK.
  echo Либо откройте проект в Android Studio и выберите Build - Build APK.
  pause
  exit /b 1
)
gradle --no-daemon clean assembleDebug
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
