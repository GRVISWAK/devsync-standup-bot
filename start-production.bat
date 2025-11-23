@echo off
REM Production Startup Script for DevSync Standup Bot (Windows)
REM Usage: start-production.bat

echo =========================================
echo  DevSync Standup Bot - Production Start
echo =========================================

REM Check if .env exists
if not exist .env (
    echo ERROR: .env file not found!
    echo Please copy .env.template to .env and configure it.
    echo.
    echo   copy .env.template .env
    echo   notepad .env
    echo.
    exit /b 1
)

REM Load environment variables
echo Loading environment variables...
for /f "usebackq tokens=*" %%a in (".env") do (
    set "%%a"
)

REM Check Java version
echo Checking Java version...
java -version 2>&1 | findstr /R "version" > nul
if errorlevel 1 (
    echo ERROR: Java not found
    exit /b 1
)
echo [OK] Java version check passed

REM Check if JAR exists
if not exist target\standup-bot-1.0.0.jar (
    echo Building application...
    call mvn clean package -DskipTests
)

REM Set JVM options for production
set JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

REM Set Spring profile
set SPRING_PROFILES_ACTIVE=production

echo.
echo Starting application...
echo Port: %SERVER_PORT%
echo Profile: production
echo Logs: logs\application.log
echo.

REM Create logs directory
if not exist logs mkdir logs

REM Start application
start "Standup Bot" java %JAVA_OPTS% -Dspring.profiles.active=production -jar target\standup-bot-1.0.0.jar

echo [OK] Application started
echo.
echo Commands:
echo   View logs:    type logs\application.log
echo   Check status: curl http://localhost:%SERVER_PORT%/api/standup/health
echo.
echo Waiting for startup...
timeout /t 5 /nobreak > nul

REM Health check
curl -s http://localhost:%SERVER_PORT%/api/standup/health > nul 2>&1
if errorlevel 1 (
    echo [WARN] Application may still be starting...
    echo   Check logs in logs\application.log
) else (
    echo [OK] Application is healthy!
)

echo.
echo =========================================
echo  Application started successfully!
echo =========================================
pause
