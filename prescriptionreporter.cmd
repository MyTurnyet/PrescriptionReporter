@REM Launcher for PrescriptionReporter on Windows, so the program can be invoked the way
@REM the brief describes it:
@REM
@REM   prescriptionreporter <file>
@REM   type <file> | prescriptionreporter
@REM
@REM Build the jar first with mvnw.cmd package.
@REM
@REM Arguments and standard input are forwarded unchanged, and the program's own exit status
@REM (0 report written, 1 input/output failure, 2 usage error) is propagated to the caller.
@echo off
setlocal

set "APP_HOME=%~dp0"
set "JAR=%APP_HOME%target\PrescriptionReporter.jar"

if not exist "%JAR%" (
    echo prescriptionreporter: jar not built at %JAR% 1>&2
    echo prescriptionreporter: run "mvnw.cmd package" first 1>&2
    exit /b 127
)

if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java.exe"
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo prescriptionreporter: cannot run a JVM ^(%JAVA_CMD%^) 1>&2
    echo prescriptionreporter: set JAVA_HOME or put java on PATH ^(Java 17 or newer^) 1>&2
    exit /b 127
)

"%JAVA_CMD%" -jar "%JAR%" %*
exit /b %ERRORLEVEL%
