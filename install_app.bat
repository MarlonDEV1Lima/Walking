@echo off
echo Compilando e instalando app no dispositivo...

echo.
echo [1/3] Compilando APK...
call gradlew assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Falha na compilacao!
    pause
    exit /b 1
)

echo.
echo [2/3] Verificando dispositivos conectados...
"%ANDROID_HOME%\platform-tools\adb.exe" devices

echo.
echo [3/3] Instalando no dispositivo...
"%ANDROID_HOME%\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ APP INSTALADO COM SUCESSO!
    echo ✅ Pode usar o app no seu celular agora.
) else (
    echo.
    echo ❌ ERRO na instalacao. Verifique se:
    echo    - Celular esta conectado via USB
    echo    - Depuracao USB esta ativada
    echo    - Dispositivo aparece na lista acima
)

echo.
pause
