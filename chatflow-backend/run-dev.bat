@echo off
setlocal

set JAVA_TOOL_OPTIONS=

cd /d "%~dp0"
call gradlew.bat bootRun --console=plain --quiet
