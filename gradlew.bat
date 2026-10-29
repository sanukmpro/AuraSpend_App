@echo off
setlocal
set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%android-app"
if exist gradlew.bat (
    call gradlew.bat %*
) else (
    echo Error: Gradle Wrapper not found in android-app directory.
)
endlocal
