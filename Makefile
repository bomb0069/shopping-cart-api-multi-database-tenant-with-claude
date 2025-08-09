# Multi-Tenant Shopping Cart Platform Makefile

.PHONY: help build start stop clean dev logs test

# Default target
help:
	@echo "Multi-Tenant Shopping Cart Platform"
	@echo "===================================="
	@echo ""
	@echo "Available commands:"
	@echo "  make build     - Build the application and Docker images"
	@echo "  make start     - Start all services (application + databases + nginx)"
	@echo "  make stop      - Stop all services"
	@echo "  make clean     - Stop services and remove volumes"
	@echo "  make dev       - Start databases only for development"
	@echo "  make dev-stop  - Stop development databases"
	@echo "  make logs      - View application logs"
	@echo "  make test      - Run tests"
	@echo "  make maven-*   - Maven commands (compile, package, etc.)"
	@echo ""

# Build application
build:
	@echo "🔨 Building application..."
	mvn clean package -DskipTests
	docker-compose build

# Start all services
start:
	@echo "🚀 Starting all services..."
	./start.sh

# Stop all services
stop:
	@echo "🛑 Stopping all services..."
	./stop.sh

# Clean up everything
clean:
	@echo "🧹 Cleaning up..."
	docker-compose down -v
	docker system prune -f

# Development environment (databases only)
dev:
	@echo "🔧 Starting development environment..."
	./dev.sh

# Stop development environment
dev-stop:
	@echo "🛑 Stopping development environment..."
	docker-compose -f docker-compose.dev.yml down

# View logs
logs:
	docker-compose logs -f

# View application logs only
logs-app:
	docker-compose logs -f shopping-cart-app

# Run tests
test:
	mvn test

# Run tests with coverage
test-coverage:
	mvn clean test jacoco:report

# Run integration tests
test-integration:
	mvn clean verify

# Run all tests (unit + integration)
test-all:
	mvn clean verify jacoco:report

# View test coverage report
coverage-report:
	@echo "Opening test coverage report..."
	@open target/site/jacoco/index.html || xdg-open target/site/jacoco/index.html || echo "Coverage report available at target/site/jacoco/index.html"

# Maven commands
maven-compile:
	mvn compile

maven-package:
	mvn package

maven-install:
	mvn install

maven-clean:
	mvn clean

# Check service health
health:
	@echo "🔍 Checking service health..."
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Application not responding"

# Test API endpoints
test-api:
	@echo "🧪 Testing API endpoints..."
	@echo "Testing Tenant 1:"
	@curl -s -H "X-Tenant-ID: tenant1" http://localhost:8080/api/products/tenant || echo "Failed"
	@echo ""
	@echo "Testing Tenant 2:"
	@curl -s -H "X-Tenant-ID: tenant2" http://localhost:8080/api/products/tenant || echo "Failed"

# Database connections
db-connect-default:
	docker exec -it $$(docker-compose ps -q mysql-default) mysql -u root -prootpassword default_db

db-connect-tenant1:
	docker exec -it $$(docker-compose ps -q mysql-tenant1) mysql -u root -prootpassword tenant1_db

db-connect-tenant2:
	docker exec -it $$(docker-compose ps -q mysql-tenant2) mysql -u root -prootpassword tenant2_db

# Show running services
status:
	docker-compose ps

# Follow all logs
tail:
	docker-compose logs -f --tail=100