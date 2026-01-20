@echo off
setlocal

rem 1. Switch to the directory where this script looks (Project Folder)
cd /d "%~dp0"

rem 2. Check for Java and Gradle
rem We check if they are in the PATH.
rem (On Windows we usually rely on PATH rather than hardcoded locations)
where java >nul 2>nul
if %errorlevel% neq 0 goto :MissingReqs

where gradle >nul 2>nul
if %errorlevel% neq 0 goto :MissingReqs

rem 3. Run the App
rem "start /B" starts it without blocking this script.
rem We use "call" to ensure gradle launches correctly.
start /B gradle klikr
exit

:MissingReqs
rem 4. Show Error Popup (Using PowerShell for the UI)
powershell -Command "Add-Type -AssemblyName PresentationFramework;[System.Windows.MessageBox]::Show('You must install Java 25 bundled with javaFX25 and Gradle 9.2 and add them to your PATH.', 'Missing Requirements', 'OK', 'Error')"
exit /b 1

