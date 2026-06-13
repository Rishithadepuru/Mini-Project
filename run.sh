#!/bin/bash
# ============================================================
# Hospital Emergency Queue — Quick Start Script (Linux/Mac)
# ============================================================
set -e

echo ""
echo "🏥 Hospital Emergency Queue Management System"
echo "=============================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+."
    exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.8+."
    exit 1
fi
echo "✅ Maven: $(mvn -version | head -1)"

echo ""
echo "📦 Building backend..."
cd backend
mvn clean package -q -DskipTests

echo ""
echo "🚀 Starting Spring Boot server on http://localhost:8080 ..."
echo "   Open frontend/index.html in your browser."
echo "   Press Ctrl+C to stop."
echo ""

mvn spring-boot:run
