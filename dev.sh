#!/bin/bash

# Development Setup Script - Start only databases for local development

set -e

echo "🔧 Starting Development Environment (Databases Only)..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Start databases for development
print_status "Starting MySQL databases for development..."
docker-compose -f docker-compose.dev.yml up -d

print_status "Waiting for databases to be ready..."
sleep 20

print_success "Development databases are ready!"

echo ""
print_status "📊 Database Connections:"
echo "   • Default DB: localhost:3306 (user: root, password: rootpassword)"
echo "   • Tenant1 DB: localhost:3307 (user: root, password: rootpassword)"  
echo "   • Tenant2 DB: localhost:3308 (user: root, password: rootpassword)"
echo "   • PhpMyAdmin: http://localhost:8081"

echo ""
print_status "🚀 Now you can run the Spring Boot application locally with:"
echo "   • mvn spring-boot:run -Dspring-boot.run.profiles=docker"
echo "   • or use your IDE with profile 'docker'"

echo ""
print_status "🛑 To stop databases:"
echo "   • docker-compose -f docker-compose.dev.yml down"

print_success "Development environment is ready! 🎉"