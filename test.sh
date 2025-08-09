#!/bin/bash

# Multi-Tenant Shopping Cart Testing Script

set -e

echo "🧪 Running Multi-Tenant Shopping Cart Tests..."

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

# Parse command line arguments
TEST_TYPE="${1:-all}"
SKIP_BUILD="${2:-false}"

print_status "Test type: $TEST_TYPE"

# Clean previous test results
print_status "Cleaning previous test results..."
mvn clean

case "$TEST_TYPE" in
    "unit")
        print_status "Running unit tests only..."
        mvn test -Dspring.profiles.active=test
        ;;
    "integration")
        print_status "Running integration tests only..."
        mvn verify -DskipUnitTests=true -Dspring.profiles.active=integration
        ;;
    "coverage")
        print_status "Running tests with coverage report..."
        mvn test jacoco:report -Dspring.profiles.active=test
        print_status "Coverage report generated at: target/site/jacoco/index.html"
        ;;
    "all")
        print_status "Running all tests (unit + integration)..."
        mvn verify jacoco:report -Dspring.profiles.active=test
        
        # Display test summary
        print_status "Test Summary:"
        if [ -f "target/surefire-reports/TEST-TestSuite.xml" ]; then
            grep -o 'tests="[^"]*"' target/surefire-reports/TEST-*.xml | head -1 || echo "Unit test count not available"
        fi
        
        if [ -f "target/failsafe-reports/TEST-TestSuite.xml" ]; then
            grep -o 'tests="[^"]*"' target/failsafe-reports/TEST-*.xml | head -1 || echo "Integration test count not available"
        fi
        ;;
    "docker")
        print_status "Running tests in Docker environment..."
        # Start test databases
        docker-compose -f docker-compose.dev.yml up -d
        sleep 20
        
        # Run tests with Docker profile
        mvn verify -Dspring.profiles.active=docker
        
        # Stop test databases
        docker-compose -f docker-compose.dev.yml down
        ;;
    *)
        print_error "Unknown test type: $TEST_TYPE"
        print_status "Available test types:"
        echo "  unit        - Run unit tests only"
        echo "  integration - Run integration tests only"
        echo "  coverage    - Run tests with coverage report"
        echo "  all         - Run all tests (default)"
        echo "  docker      - Run tests with Docker databases"
        exit 1
        ;;
esac

# Check test results
if [ $? -eq 0 ]; then
    print_success "All tests passed! ✅"
    
    # Display coverage summary if available
    if [ -f "target/site/jacoco/index.html" ]; then
        print_status "📊 Test coverage report is available at:"
        echo "   file://$(pwd)/target/site/jacoco/index.html"
        
        # Try to extract coverage percentage (basic implementation)
        if command -v grep &> /dev/null && [ -f "target/site/jacoco/jacoco.csv" ]; then
            print_status "Coverage summary:"
            tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '{
                covered += $5 + $7 + $9
                missed += $4 + $6 + $8
            } END {
                total = covered + missed
                if (total > 0) {
                    percentage = (covered * 100) / total
                    printf "   Line Coverage: %.1f%% (%d/%d lines)\n", percentage, covered, total
                }
            }'
        fi
    fi
    
    print_status "🎯 Next steps:"
    echo "   • Review test results in target/surefire-reports/"
    echo "   • Check coverage report at target/site/jacoco/index.html"
    echo "   • Run 'make coverage-report' to open coverage in browser"
    
else
    print_error "Some tests failed! ❌"
    print_status "Check the logs above for details"
    print_status "Test reports are available at:"
    echo "   • Unit tests: target/surefire-reports/"
    echo "   • Integration tests: target/failsafe-reports/"
    exit 1
fi