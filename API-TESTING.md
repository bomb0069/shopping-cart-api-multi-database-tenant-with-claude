# API Testing Guide - Multi-Tenant Shopping Cart Platform

This guide provides comprehensive information about API testing for the multi-tenant shopping cart platform using Postman collections and Newman automation.

## Overview

The API testing suite includes:
- **Comprehensive Postman Collections** - Full API coverage with automated tests
- **Newman Integration** - Command-line test execution and CI/CD integration
- **Multi-Tenant Validation** - Ensures proper tenant isolation
- **Automated Test Reports** - HTML reports with detailed results

## Quick Start

### Prerequisites
- Node.js and npm installed
- Application running (locally or Docker)
- Newman CLI tool (auto-installed by scripts)

### Running API Tests

```bash
# Install Newman (if not already installed)
make install-newman

# Run API tests against local environment
make api-test

# Run API tests against Docker environment  
make api-test-docker

# Run API tests with performance testing
make api-test-performance

# Manual execution
./api-test.sh local http://localhost:8080
```

## Test Structure

### Collection Organization

The Postman collection is organized into logical folders:

```
Multi-Tenant Shopping Cart API/
├── Health Check/
│   ├── Application Health
│   └── Current Tenant Info
├── Products API/
│   ├── Get All Products
│   ├── Create Product
│   ├── Get Product by ID
│   ├── Search Products
│   └── Update Product
├── Prices API/
│   ├── Get Effective Price
│   └── Create Price Rule
├── Promotions API/
│   ├── Get Active Promotions
│   ├── Create Promotion
│   └── Calculate Discount
├── Cart API/
│   ├── Get Cart
│   ├── Add Item to Cart
│   ├── Update Cart Item
│   └── Apply Promotion
├── Multi-Tenant Isolation Tests/
│   ├── Create Product in Tenant1
│   ├── Create Product in Tenant2
│   ├── Verify Tenant1 Cannot See Tenant2 Product
│   └── Verify Same SKU Allowed Across Tenants
└── Cleanup/
    ├── Delete Test Product
    └── Clear Cart
```

### Test Coverage

#### Functional Testing
- ✅ **CRUD Operations** - Create, Read, Update, Delete for all entities
- ✅ **Business Logic** - Price calculations, promotions, cart management
- ✅ **Search & Filtering** - Product search, category filtering
- ✅ **Validation** - Input validation and error handling
- ✅ **Data Consistency** - Proper data relationships

#### Multi-Tenant Testing
- ✅ **Tenant Isolation** - Data separation between tenants
- ✅ **Tenant Context** - Proper tenant resolution
- ✅ **Cross-Tenant Access** - Preventing unauthorized access
- ✅ **Duplicate Data** - Same SKUs across tenants

#### Integration Testing
- ✅ **API Workflows** - End-to-end user journeys
- ✅ **State Management** - Session handling, cart persistence
- ✅ **Error Scenarios** - 404s, validation errors, conflicts

## Environments

### Local Environment
```json
{
  "base_url": "http://localhost:8080",
  "tenant_id": "tenant1"
}
```

### Docker Environment  
```json
{
  "base_url": "http://localhost:8080",
  "tenant_id": "tenant1"
}
```

### CI Environment
```json
{
  "base_url": "http://localhost:8080", 
  "tenant_id": "tenant1"
}
```

## Test Execution

### Local Testing

#### Prerequisites Check
```bash
# Check if application is running
curl http://localhost:8080/actuator/health

# Check tenant context
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/products/tenant
```

#### Manual Newman Execution
```bash
# Basic run
newman run api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json \
  -e api-tests/environments/local.postman_environment.json

# With HTML report
newman run api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json \
  -e api-tests/environments/local.postman_environment.json \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export api-test-report.html

# Run specific folder
newman run api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json \
  -e api-tests/environments/local.postman_environment.json \
  --folder "Products API"

# Performance testing (multiple iterations)
newman run api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json \
  -e api-tests/environments/local.postman_environment.json \
  -n 10 \
  --delay-request 100
```

### CI/CD Integration

The GitHub Actions workflow includes automated API testing:

```yaml
api-tests:
  needs: build
  runs-on: ubuntu-latest
  
  services:
    mysql-default:
      image: mysql:8.0
      # ... database configuration
  
  steps:
    - name: Start application
      run: |
        java -jar target/multi-tenant-shopping-cart-1.0.0-SNAPSHOT.jar &
    
    - name: Run API tests
      run: |
        newman run api-tests/Multi-Tenant-Shopping-Cart.postman_collection.json \
          -e api-tests/environments/ci.postman_environment.json \
          --reporters cli,htmlextra \
          --bail
```

## Test Data Management

### Dynamic Test Data
Tests use dynamic data generation to avoid conflicts:
```javascript
// SKU with random number
"sku": "API-TEST-{{$randomInt}}"

// Promotion code with timestamp
"code": "APITEST{{$randomInt}}"

// Current timestamp for date fields
"validFrom": "{{$isoTimestamp}}"
```

### Environment Variables
Tests store created entities for reuse:
```javascript
// Store product ID for subsequent tests
pm.environment.set('created_product_id', response.id);

// Use stored ID in next request
"{{created_product_id}}"
```

### Cleanup
Tests include cleanup procedures to maintain environment state:
- Delete created test data
- Clear shopping carts
- Reset tenant contexts

## Test Assertions

### Status Code Validation
```javascript
pm.test('Product created successfully', function () {
    pm.response.to.have.status(201);
});
```

### Response Structure Validation
```javascript
pm.test('Product has correct structure', function () {
    const response = pm.response.json();
    pm.expect(response).to.have.all.keys(
        'id', 'name', 'sku', 'basePrice', 'active'
    );
});
```

### Business Logic Validation
```javascript
pm.test('Discount calculated correctly', function () {
    const discount = parseFloat(pm.response.text());
    pm.expect(discount).to.eql(10); // 10% of 100
});
```

### Tenant Isolation Validation
```javascript
pm.test('Tenant isolation maintained', function () {
    pm.response.to.have.status(404);
});
```

## Reports and Analytics

### HTML Reports
Newman generates detailed HTML reports with:
- **Test Results** - Pass/fail status for each test
- **Response Times** - Performance metrics
- **Request/Response Details** - Full HTTP transaction logs
- **Test Scripts** - JavaScript test code
- **Environment Variables** - Runtime configuration

### CI Integration
- **Test artifacts** uploaded to GitHub Actions
- **Slack notifications** for test failures (configurable)
- **Test result badges** in README
- **Coverage metrics** integration

## Troubleshooting

### Common Issues

#### Application Not Running
```bash
# Error: ECONNREFUSED
# Solution: Start the application
./start.sh
# or
mvn spring-boot:run
```

#### Newman Not Found
```bash
# Error: command not found: newman
# Solution: Install Newman
npm install -g newman newman-reporter-htmlextra
```

#### Test Timeouts
```bash
# Increase timeout in Newman command
newman run ... --timeout-request 10000
```

#### Tenant Context Issues
```bash
# Verify tenant header is set correctly
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/products/tenant
```

### Debugging Tests

#### Verbose Output
```bash
newman run ... --verbose
```

#### Debug Individual Requests
```bash
# Run single request
newman run collection.json --folder "Products API" --verbose
```

#### Environment Variables
```javascript
// Log environment variable
console.log("Product ID:", pm.environment.get('created_product_id'));
```

## Best Practices

### Test Organization
- **Logical Grouping** - Group related tests in folders
- **Sequential Dependencies** - Order tests to build on each other
- **Cleanup** - Always clean up test data
- **Idempotency** - Tests should be repeatable

### Error Handling
```javascript
pm.test('Handle validation errors gracefully', function () {
    if (pm.response.code === 400) {
        const error = pm.response.json();
        pm.expect(error).to.have.property('message');
    } else {
        pm.response.to.have.status(201);
    }
});
```

### Performance Considerations
- **Minimal Delays** - Use appropriate delay between requests
- **Parallel Execution** - Run independent tests in parallel
- **Resource Cleanup** - Avoid resource leaks
- **Connection Pooling** - Reuse connections where possible

### Security Testing
- **Authentication** - Test auth mechanisms
- **Authorization** - Verify access controls
- **Input Validation** - Test with malicious inputs
- **Data Exposure** - Check for information leaks

## Advanced Features

### Custom Reporters
```bash
# Install custom reporter
npm install -g newman-reporter-teamcity

# Use custom reporter
newman run ... --reporters teamcity
```

### Data Files
```bash
# Use CSV data file for multiple test scenarios
newman run collection.json -d test-data.csv
```

### Integration with Testing Tools

#### Postman Monitoring
- **Scheduled Runs** - Regular API health checks
- **Global Variables** - Shared configuration
- **Team Collaboration** - Shared collections

#### GitHub Integration
- **Status Checks** - Block merges on test failures
- **PR Comments** - Automated test result comments
- **Release Gates** - API tests as release criteria

## Continuous Improvement

### Metrics to Track
- **Test Coverage** - API endpoint coverage
- **Test Reliability** - Flaky test identification
- **Performance** - Response time trends
- **Failure Rates** - Success/failure ratios

### Expanding Test Suite
- **Load Testing** - High-volume scenarios
- **Security Testing** - Vulnerability assessment
- **Contract Testing** - API contract validation
- **Chaos Testing** - Resilience testing

The API testing framework provides comprehensive validation of the multi-tenant shopping cart platform, ensuring reliability, security, and proper tenant isolation across all environments.