#!/bin/bash

# Return Chatbot - Quick Start Script
# This script helps you start both the backend and frontend

echo "=========================================="
echo "Return Chatbot - Quick Start"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if backend is running
check_backend() {
    curl -s http://localhost:9090/api/chat -X POST -H "Content-Type: application/json" -d '{"prompt":"test"}' > /dev/null 2>&1
    return $?
}

# Check if frontend is running
check_frontend() {
    curl -s http://localhost:5173 > /dev/null 2>&1
    return $?
}

echo -e "${YELLOW}Checking services...${NC}"
echo ""

# Check backend
if check_backend; then
    echo -e "${GREEN}✓ Backend is running on http://localhost:9090${NC}"
else
    echo -e "${RED}✗ Backend is NOT running${NC}"
    echo -e "${YELLOW}Start the backend with:${NC}"
    echo "  cd /home/mank/IdeaProjects/Returnchatbot"
    echo "  java -jar target/Returnchatbot-0.0.1-SNAPSHOT.jar --server.port=9090"
    echo "  or"
    echo "  ./mvnw spring-boot:run -Dspring-boot.run.arguments='--server.port=9090'"
    echo ""
fi

# Check frontend
if check_frontend; then
    echo -e "${GREEN}✓ Frontend is running on http://localhost:5173${NC}"
else
    echo -e "${RED}✗ Frontend is NOT running${NC}"
    echo -e "${YELLOW}Start the frontend with:${NC}"
    echo "  cd /home/mank/IdeaProjects/Returnchatbot/returnchatbot-frontend"
    echo "  npm run dev"
    echo ""
fi

echo ""
echo "=========================================="
echo "Available Commands:"
echo "=========================================="
echo ""
echo "Start Backend:"
echo "  cd /home/mank/IdeaProjects/Returnchatbot"
echo "  java -jar target/Returnchatbot-0.0.1-SNAPSHOT.jar --server.port=9090"
echo ""
echo "Start Frontend:"
echo "  cd /home/mank/IdeaProjects/Returnchatbot/returnchatbot-frontend"
echo "  npm run dev"
echo ""
echo "Open in Browser:"
echo "  http://localhost:5173"
echo ""
echo "Test Backend API:"
echo "  curl -X POST http://localhost:9090/api/chat \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"prompt\": \"Hello\"}'"
echo ""

