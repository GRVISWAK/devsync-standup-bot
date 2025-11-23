@echo off
echo ========================================
echo DevSync Standup Bot - Starting...
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    pause
    exit /b 1
)

echo Java and Maven detected successfully!
echo.
echo Building and starting the application...
echo.

REM Run the application
mvn spring-boot:run

pause
