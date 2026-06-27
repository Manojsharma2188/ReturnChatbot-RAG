#!/bin/bash

# PostgreSQL Setup Script for Returnchatbot
# This script automates the database setup process

set -e  # Exit on error

echo "========================================="
echo "PostgreSQL Database Setup for Returnchatbot"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Check if PostgreSQL is installed
echo -e "${YELLOW}[STEP 1] Checking PostgreSQL installation...${NC}"
if command -v psql &> /dev/null; then
    PSQL_VERSION=$(psql --version)
    echo -e "${GREEN}✓ PostgreSQL found: $PSQL_VERSION${NC}"
else
    echo -e "${RED}✗ PostgreSQL not found. Please install PostgreSQL first.${NC}"
    exit 1
fi
echo ""

# Step 2: Check PostgreSQL service
echo -e "${YELLOW}[STEP 2] Checking PostgreSQL service status...${NC}"
if command -v systemctl &> /dev/null; then
    if systemctl is-active --quiet postgresql; then
        echo -e "${GREEN}✓ PostgreSQL service is running${NC}"
    else
        echo -e "${YELLOW}! PostgreSQL service is not running. Starting...${NC}"
        sudo systemctl start postgresql
        sleep 2
        echo -e "${GREEN}✓ PostgreSQL service started${NC}"
    fi
elif command -v brew &> /dev/null; then
    # macOS with Homebrew
    echo -e "${GREEN}✓ Assuming PostgreSQL is running (macOS)${NC}"
else
    echo -e "${YELLOW}! Cannot verify service status on this system${NC}"
fi
echo ""

# Step 3: Get PostgreSQL credentials
echo -e "${YELLOW}[STEP 3] PostgreSQL Connection Configuration${NC}"
read -p "Enter PostgreSQL username [postgres]: " PG_USER
PG_USER=${PG_USER:-postgres}

read -sp "Enter PostgreSQL password (press Enter if no password): " PG_PASSWORD
echo ""

# Test connection
echo -e "${YELLOW}Testing connection...${NC}"
if [ -z "$PG_PASSWORD" ]; then
    if PGPASSWORD="" psql -U "$PG_USER" -c "\q" 2>/dev/null; then
        echo -e "${GREEN}✓ Connection successful (no password)${NC}"
        CONNECT_CMD="psql -U $PG_USER"
    else
        echo -e "${RED}✗ Connection failed. Please check credentials.${NC}"
        exit 1
    fi
else
    if PGPASSWORD="$PG_PASSWORD" psql -U "$PG_USER" -c "\q" 2>/dev/null; then
        echo -e "${GREEN}✓ Connection successful${NC}"
        CONNECT_CMD="PGPASSWORD='$PG_PASSWORD' psql -U $PG_USER"
    else
        echo -e "${RED}✗ Connection failed. Please check credentials.${NC}"
        exit 1
    fi
fi
echo ""

# Step 4: Create database
echo -e "${YELLOW}[STEP 4] Creating 'returnchatbot' database...${NC}"
CREATE_DB_CMD="CREATE DATABASE returnchatbot WITH ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' TEMPLATE = template0;"

if eval "$CONNECT_CMD -c \"$CREATE_DB_CMD\"" 2>/dev/null; then
    echo -e "${GREEN}✓ Database 'returnchatbot' created successfully${NC}"
else
    echo -e "${YELLOW}! Database 'returnchatbot' might already exist${NC}"
fi
echo ""

# Step 5: Run schema file
echo -e "${YELLOW}[STEP 5] Running database schema...${NC}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SCHEMA_FILE="$SCRIPT_DIR/DATABASE_SCHEMA_POSTGRESQL.sql"

if [ ! -f "$SCHEMA_FILE" ]; then
    echo -e "${RED}✗ Schema file not found: $SCHEMA_FILE${NC}"
    exit 1
fi

if eval "$CONNECT_CMD -d returnchatbot -f \"$SCHEMA_FILE\"" 2>/dev/null; then
    echo -e "${GREEN}✓ Database schema created successfully${NC}"
else
    echo -e "${RED}✗ Error creating schema. Check file permissions or database access.${NC}"
    exit 1
fi
echo ""

# Step 6: Verify tables
echo -e "${YELLOW}[STEP 6] Verifying tables...${NC}"
TABLE_COUNT=$(eval "$CONNECT_CMD -d returnchatbot -t -c \"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'chatbot';\"" 2>/dev/null | xargs)

if [ "$TABLE_COUNT" -eq 8 ]; then
    echo -e "${GREEN}✓ All 8 tables created successfully${NC}"
else
    echo -e "${YELLOW}! Found $TABLE_COUNT tables (expected 8)${NC}"
fi

# List tables
echo -e "${YELLOW}Tables:${NC}"
eval "$CONNECT_CMD -d returnchatbot -c \"\\dt\"" 2>/dev/null
echo ""

# Step 7: Verify views
echo -e "${YELLOW}[STEP 7] Verifying views...${NC}"
VIEW_COUNT=$(eval "$CONNECT_CMD -d returnchatbot -t -c \"SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'chatbot';\"" 2>/dev/null | xargs)

if [ "$VIEW_COUNT" -eq 3 ]; then
    echo -e "${GREEN}✓ All 3 views created successfully${NC}"
else
    echo -e "${YELLOW}! Found $VIEW_COUNT views (expected 3)${NC}"
fi

# List views
echo -e "${YELLOW}Views:${NC}"
eval "$CONNECT_CMD -d returnchatbot -c \"\\dv\"" 2>/dev/null
echo ""

# Step 8: Setup complete message
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}✓ PostgreSQL Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Update application.properties with:"
echo "   spring.datasource.url=jdbc:postgresql://localhost:5432/returnchatbot"
echo "   spring.datasource.username=$PG_USER"
echo "   spring.datasource.password=<your_password>"
echo ""
echo "2. Update pom.xml with PostgreSQL driver dependency"
echo "3. Run: mvn clean install"
echo "4. Start Spring Boot: mvn spring-boot:run"
echo ""
echo "Database Info:"
echo "  Database Name: returnchatbot"
echo "  Username: $PG_USER"
echo "  Host: localhost"
echo "  Port: 5432"
echo ""
echo "Useful commands:"
echo "  Connect: psql -U $PG_USER -d returnchatbot"
echo "  Backup: pg_dump -U $PG_USER returnchatbot > backup.sql"
echo "  Restore: psql -U $PG_USER returnchatbot < backup.sql"
echo ""

