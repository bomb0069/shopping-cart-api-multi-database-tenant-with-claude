#!/bin/bash

# Multi-Tenant Shopping Cart Platform Startup Script

set -e

echo "🚀 Starting Multi-Tenant Shopping Cart Platform..."

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

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install docker-compose first."
    exit 1
fi

# Create logs directory
mkdir -p logs

print_status "Building and starting services..."

# Build and start all services
docker-compose up --build -d

print_status "Waiting for services to be healthy..."

# Wait for databases to be ready
print_status "Waiting for MySQL databases to be ready..."
sleep 30

# Check if services are running
if docker-compose ps | grep -q "Up"; then
    print_success "Services are running!"
    
    echo ""
    print_status "🌐 Application URLs:"
    echo "   • Application: http://localhost:8080"
    echo "   • Nginx Proxy: http://localhost:80"
    echo "   • PhpMyAdmin: http://localhost:8081"
    echo "   • Health Check: http://localhost:8080/actuator/health"
    
    echo ""
    print_status "📊 Database Connections:"
    echo "   • Default DB: localhost:3306"
    echo "   • Tenant1 DB: localhost:3307"
    echo "   • Tenant2 DB: localhost:3308"
    
    echo ""
    print_status "🔧 Testing API Endpoints:"
    echo "   • Test Tenant 1: curl -H 'X-Tenant-ID: tenant1' http://localhost:8080/api/products"
    echo "   • Test Tenant 2: curl -H 'X-Tenant-ID: tenant2' http://localhost:8080/api/products"
    echo "   • Test via Nginx: curl http://localhost/api/products"
    
    echo ""
    print_status "📝 View logs with:"
    echo "   • docker-compose logs -f"
    echo "   • docker-compose logs -f shopping-cart-app"
    
else
    print_error "Some services failed to start. Check logs with: docker-compose logs"
    exit 1
fi

print_success "Multi-Tenant Shopping Cart Platform is ready! 🎉"