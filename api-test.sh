#!/bin/bash

# Multi-Tenant Shopping Cart API Testing Script

set -e

echo "🧪 API Testing for Multi-Tenant Shopping Cart..."

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

# Configuration
ENVIRONMENT="${1:-local}"
BASE_URL="${2:-http://localhost:8080}"
COLLECTION_FILE="api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json"
ENVIRONMENT_FILE="api-tests/environments/${ENVIRONMENT}.postman_environment.json"
REPORT_FILE="api-test-results-$(date +%Y%m%d_%H%M%S).html"

print_status "Environment: $ENVIRONMENT"
print_status "Base URL: $BASE_URL"
print_status "Report file: $REPORT_FILE"

# Check if Newman is installed
if ! command -v newman &> /dev/null; then
    print_error "Newman is not installed. Installing..."
    
    if command -v npm &> /dev/null; then
        npm install -g newman newman-reporter-htmlextra
    else
        print_error "npm is not installed. Please install Node.js and npm first."
        print_status "Installation instructions:"
        echo "  • macOS: brew install node"
        echo "  • Ubuntu: sudo apt install nodejs npm"
        echo "  • Windows: Download from https://nodejs.org"
        exit 1
    fi
fi

# Check if collection file exists
if [ ! -f "$COLLECTION_FILE" ]; then
    print_error "Collection file not found: $COLLECTION_FILE"
    exit 1
fi

# Check if environment file exists
if [ ! -f "$ENVIRONMENT_FILE" ]; then
    print_error "Environment file not found: $ENVIRONMENT_FILE"
    print_status "Available environments:"
    ls -la api-tests/environments/ 2>/dev/null || echo "No environment files found"
    exit 1
fi

# Function to check if application is running
check_application() {
    local max_attempts=30
    local attempt=1
    
    print_status "Checking if application is running at $BASE_URL..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            print_success "Application is running and healthy!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts - Application not ready, waiting..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "Application is not responding at $BASE_URL"
    print_status "Please ensure the application is running:"
    echo "  • Local: ./start.sh or mvn spring-boot:run"
    echo "  • Docker: ./start.sh"
    echo "  • Manual: java -jar target/multi-tenant-shopping-cart-1.0.0-SNAPSHOT.jar"
    return 1
}

# Check application health
if ! check_application; then
    exit 1
fi

# Create reports directory
mkdir -p api-test-reports

# Run Newman tests
print_status "Running API tests with Newman..."

# Basic Newman run
newman run "$COLLECTION_FILE" \
    -e "$ENVIRONMENT_FILE" \
    --reporters cli,htmlextra \
    --reporter-htmlextra-export "api-test-reports/$REPORT_FILE" \
    --reporter-htmlextra-darkTheme \
    --reporter-htmlextra-title "Multi-Tenant Shopping Cart API Tests" \
    --reporter-htmlextra-showOnlyFails \
    --timeout-request 10000 \
    --delay-request 500 \
    --color on \
    --disable-unicode \
    --verbose

# Check if tests passed
if [ $? -eq 0 ]; then
    print_success "All API tests passed! ✅"
    
    # Display report location
    print_status "📊 Test report generated:"
    echo "   file://$(pwd)/api-test-reports/$REPORT_FILE"
    
    # Try to open report automatically
    if command -v open &> /dev/null; then
        print_status "Opening report in browser..."
        open "api-test-reports/$REPORT_FILE"
    elif command -v xdg-open &> /dev/null; then
        print_status "Opening report in browser..."
        xdg-open "api-test-reports/$REPORT_FILE"
    fi
    
    # Run additional tenant isolation tests
    print_status "🔒 Running additional tenant isolation tests..."
    
    newman run "$COLLECTION_FILE" \
        -e "$ENVIRONMENT_FILE" \
        --folder "Multi-Tenant Isolation Tests" \
        --reporters cli \
        --timeout-request 5000 \
        --delay-request 250 \
        --color on
    
    if [ $? -eq 0 ]; then
        print_success "Tenant isolation tests passed! 🛡️"
    else
        print_error "Tenant isolation tests failed! ❌"
        exit 1
    fi
    
    print_status "🎯 API Testing Summary:"
    echo "   • All functional tests: PASSED"
    echo "   • Multi-tenant isolation: VERIFIED"
    echo "   • Test report: api-test-reports/$REPORT_FILE"
    
else
    print_error "API tests failed! ❌"
    
    print_status "📋 Troubleshooting:"
    echo "   • Check application logs"
    echo "   • Verify database connections"
    echo "   • Review test report: api-test-reports/$REPORT_FILE"
    echo "   • Check network connectivity"
    
    exit 1
fi

# Optional: Run performance tests if requested
if [ "$3" == "--performance" ]; then
    print_status "🚀 Running performance tests..."
    
    newman run "$COLLECTION_FILE" \
        -e "$ENVIRONMENT_FILE" \
        --folder "Products API" \
        -n 10 \
        --reporters cli \
        --timeout-request 5000 \
        --delay-request 100 \
        --color on
    
    print_status "Performance test completed"
fi

print_success "API testing completed successfully! 🎉"