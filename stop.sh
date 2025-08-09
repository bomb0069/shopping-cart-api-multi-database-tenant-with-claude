#!/bin/bash

# Multi-Tenant Shopping Cart Platform Stop Script

set -e

echo "🛑 Stopping Multi-Tenant Shopping Cart Platform..."

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

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed."
    exit 1
fi

# Stop all services
print_status "Stopping all services..."
docker-compose down

# Optionally remove volumes (uncomment if you want to clean data)
# print_status "Removing volumes..."
# docker-compose down -v

print_success "All services stopped successfully! 👋"

print_status "To completely clean up (remove volumes and data):"
echo "   docker-compose down -v"
echo "   docker system prune -f"