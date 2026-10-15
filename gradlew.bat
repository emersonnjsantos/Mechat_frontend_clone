@rem WhatsApp Clone Gradle Launcher
@echo off
set "GRADLE_BIN=C:\Users\emer7\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat"
if exist "%GRADLE_BIN%" (
    call "%GRADLE_BIN%" %*
) else (
    gradle %*
)
