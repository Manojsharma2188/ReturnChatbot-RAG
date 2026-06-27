@echo off
REM PostgreSQL Setup Script for Returnchatbot (Windows)
REM This script automates the database setup process

setlocal enabledelayedexpansion

echo =========================================
echo PostgreSQL Database Setup for Returnchatbot
echo =========================================
echo.

REM Step 1: Check if PostgreSQL is installed
echo [STEP 1] Checking PostgreSQL installation...
where psql >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: PostgreSQL not found. Please install PostgreSQL first.
    pause
    exit /b 1
)

for /f "tokens=*" %%a in ('psql --version') do set PG_VERSION=%%a
echo PostgreSQL found: %PG_VERSION%
echo.

REM Step 2: Get PostgreSQL credentials
echo [STEP 2] PostgreSQL Connection Configuration
set /p PG_USER="Enter PostgreSQL username [postgres]: "
if "!PG_USER!"=="" set PG_USER=postgres

set /p PG_PASSWORD="Enter PostgreSQL password (press Enter if no password): "
echo.

REM Step 3: Test connection
echo [STEP 3] Testing connection...
if "!PG_PASSWORD!"=="" (
    psql -U !PG_USER! -c "\q" >nul 2>nul
) else (
    set PGPASSWORD=!PG_PASSWORD!
    psql -U !PG_USER! -c "\q" >nul 2>nul
)

if %errorlevel% neq 0 (
    echo Error: Connection failed. Please check credentials.
    pause
    exit /b 1
)
echo Connection successful!
echo.

REM Step 4: Create database
echo [STEP 4] Creating 'returnchatbot' database...

if "!PG_PASSWORD!"=="" (
    psql -U !PG_USER! -c "CREATE DATABASE returnchatbot WITH ENCODING = 'UTF8';" >nul 2>nul
) else (
    set PGPASSWORD=!PG_PASSWORD!
    psql -U !PG_USER! -c "CREATE DATABASE returnchatbot WITH ENCODING = 'UTF8';" >nul 2>nul
)

if %errorlevel% equ 0 (
    echo Database 'returnchatbot' created successfully
) else (
    echo Database 'returnchatbot' might already exist
)
echo.

REM Step 5: Run schema file
echo [STEP 5] Running database schema...

set SCHEMA_FILE=%~dp0DATABASE_SCHEMA_POSTGRESQL.sql

if not exist "!SCHEMA_FILE!" (
    echo Error: Schema file not found: !SCHEMA_FILE!
    pause
    exit /b 1
)

if "!PG_PASSWORD!"=="" (
    psql -U !PG_USER! -d returnchatbot -f "!SCHEMA_FILE!" >nul 2>nul
) else (
    set PGPASSWORD=!PG_PASSWORD!
    psql -U !PG_USER! -d returnchatbot -f "!SCHEMA_FILE!" >nul 2>nul
)

if %errorlevel% equ 0 (
    echo Database schema created successfully
) else (
    echo Error creating schema. Check file permissions or database access.
    pause
    exit /b 1
)
echo.

REM Step 6: Verify tables
echo [STEP 6] Verifying tables...

if "!PG_PASSWORD!"=="" (
    psql -U !PG_USER! -d returnchatbot -c "\dt"
) else (
    set PGPASSWORD=!PG_PASSWORD!
    psql -U !PG_USER! -d returnchatbot -c "\dt"
)
echo.

REM Step 7: Verify views
echo [STEP 7] Verifying views...

if "!PG_PASSWORD!"=="" (
    psql -U !PG_USER! -d returnchatbot -c "\dv"
) else (
    set PGPASSWORD=!PG_PASSWORD!
    psql -U !PG_USER! -d returnchatbot -c "\dv"
)
echo.

REM Step 8: Setup complete
echo =========================================
echo PostgreSQL Setup Complete!
echo =========================================
echo.
echo Next steps:
echo 1. Update application.properties with:
echo    spring.datasource.url=jdbc:postgresql://localhost:5432/returnchatbot
echo    spring.datasource.username=!PG_USER!
echo    spring.datasource.password=^<your_password^>
echo.
echo 2. Update pom.xml with PostgreSQL driver dependency
echo 3. Run: mvn clean install
echo 4. Start Spring Boot: mvn spring-boot:run
echo.
echo Database Info:
echo   Database Name: returnchatbot
echo   Username: !PG_USER!
echo   Host: localhost
echo   Port: 5432
echo.
echo Useful commands:
echo   Connect: psql -U !PG_USER! -d returnchatbot
echo   Backup: pg_dump -U !PG_USER! returnchatbot ^> backup.sql
echo   Restore: psql -U !PG_USER! returnchatbot ^< backup.sql
echo.
pause

