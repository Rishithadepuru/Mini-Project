@echo off
REM ============================================================
REM Hospital Emergency Queue — Quick Start Script (Windows)
REM ============================================================

echo.
echo  Hospital Emergency Queue Management System
echo  ==========================================
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Java not found. Please install Java 17+.
    pause
    exit /b 1
)
echo  [OK] Java found.

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Maven not found. Please install Maven 3.8+.
    pause
    exit /b 1
)
echo  [OK] Maven found.

echo.
echo  Building backend...
cd backend
call mvn clean package -q -DskipTests

echo.
echo  Starting Spring Boot on http://localhost:8080 ...
echo  Open frontend\index.html in your browser.
echo  Press Ctrl+C to stop.
echo.

call mvn spring-boot:run
pause
