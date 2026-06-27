@echo off
REM Return Chatbot - Quick Start Script (Windows)
REM This script helps you start both the backend and frontend

echo.
echo ==========================================
echo Return Chatbot - Quick Start (Windows)
echo ==========================================
echo.

echo Checking services...
echo.

REM Check backend
echo Checking backend on http://localhost:9090...
curl -s http://localhost:9090/api/chat -X POST -H "Content-Type: application/json" -d "{\"prompt\":\"test\"}" > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend is running on http://localhost:9090
) else (
    echo [FAILED] Backend is NOT running
    echo.
    echo Start the backend with one of these commands:
    echo   cd C:\path\to\Returnchatbot
    echo   java -jar target/Returnchatbot-0.0.1-SNAPSHOT.jar --server.port=9090
    echo   or
    echo   mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
    echo.
)

REM Check frontend
echo Checking frontend on http://localhost:5173...
curl -s http://localhost:5173 > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Frontend is running on http://localhost:5173
) else (
    echo [FAILED] Frontend is NOT running
    echo.
    echo Start the frontend with:
    echo   cd C:\path\to\Returnchatbot\returnchatbot-frontend
    echo   npm run dev
    echo.
)

echo.
echo ==========================================
echo Available Commands:
echo ==========================================
echo.
echo Start Backend:
echo   cd C:\path\to\Returnchatbot
echo   java -jar target/Returnchatbot-0.0.1-SNAPSHOT.jar --server.port=9090
echo.
echo Start Frontend:
echo   cd C:\path\to\Returnchatbot\returnchatbot-frontend
echo   npm run dev
echo.
echo Open in Browser:
echo   http://localhost:5173
echo.
echo Test Backend API:
echo   curl -X POST http://localhost:9090/api/chat ^
echo     -H "Content-Type: application/json" ^
echo     -d "{\"prompt\": \"Hello\"}"
echo.
pause

