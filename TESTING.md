# Testing Guide - Multi-Tenant Shopping Cart Platform

This document provides comprehensive information about testing the multi-tenant shopping cart platform.

## Test Structure

### Test Types

1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test component interactions and API endpoints
3. **Multi-Tenant Isolation Tests** - Verify data isolation between tenants
4. **Controller Tests** - Test REST API endpoints with mock services

### Test Packages

```
src/test/java/
├── com/shoppingcart/multitenant/
│   ├── config/          # Configuration and context tests
│   ├── service/         # Business logic unit tests
│   ├── controller/      # REST API integration tests
│   └── TestConfiguration.java
└── AllTestsSuite.java   # Test suite runner
```

## Running Tests

### Command Line

```bash
# Run all tests
./test.sh all
# OR
make test-all

# Run only unit tests
./test.sh unit
# OR
make test

# Run only integration tests  
./test.sh integration
# OR
make test-integration

# Run tests with coverage
./test.sh coverage
# OR
make test-coverage

# Run tests with Docker databases
./test.sh docker
```

### Maven Commands

```bash
# Unit tests only
mvn test

# Integration tests only
mvn verify -DskipUnitTests=true

# All tests with coverage
mvn verify jacoco:report

# Skip tests
mvn package -DskipTests
```

### IDE Integration

#### IntelliJ IDEA
1. Right-click on `src/test/java` → Run 'All Tests'
2. Use the built-in test runner with JUnit 5
3. Enable coverage analysis in the test configuration

#### VS Code
1. Install Java Test Runner extension
2. Use the Test Explorer panel
3. Run tests individually or as suites

## Test Configuration

### Profiles

- **test**: Default test profile with H2 in-memory databases
- **integration**: Integration test profile with extended timeouts
- **docker**: Test profile for Docker environment

### Test Databases

The test suite uses H2 in-memory databases configured per tenant:
- `test_default_db` - Default tenant database
- `test_tenant1_db` - Tenant 1 database  
- `test_tenant2_db` - Tenant 2 database

### Test Data

Tests use programmatically created test data rather than fixtures to ensure isolation and reliability.

## Test Coverage

### Coverage Goals

- **Minimum Line Coverage**: 80%
- **Branch Coverage**: 75%
- **Method Coverage**: 90%

### Coverage Reports

After running tests with coverage:

```bash
# View coverage report
make coverage-report

# Manual access
open target/site/jacoco/index.html
```

Coverage reports include:
- Line-by-line coverage details
- Package and class summaries
- Branch coverage analysis
- Complexity metrics

## Key Test Classes

### Unit Tests

#### TenantContextTest
- Tests thread-local tenant context management
- Verifies tenant isolation between threads
- Tests context cleanup

#### ProductServiceTest  
- Tests CRUD operations for products
- Mocks repository dependencies
- Verifies business logic validation

#### PriceServiceTest
- Tests effective price calculations
- Tests customer group pricing
- Tests time-based pricing rules

#### PromotionServiceTest
- Tests discount calculations
- Tests promotion validation logic
- Tests usage limit enforcement

#### CartServiceTest
- Tests cart item management
- Tests promotion application
- Tests cart calculation logic

### Integration Tests

#### ProductControllerIntegrationTest
- Tests REST API endpoints
- Tests request/response handling
- Tests error scenarios
- Uses mock services with WebMvcTest

#### MultiTenantDataIsolationIntegrationTest
- Tests complete data isolation between tenants
- Tests tenant switching
- Tests cross-tenant data access prevention
- Uses full Spring Boot application context

## Test Utilities

### TestTenantContextHelper

Utility class for managing tenant context in tests:

```java
@Autowired
private TestTenantContextHelper tenantHelper;

// Switch tenant context
tenantHelper.setTenant("tenant1");

// Execute operation with specific tenant
tenantHelper.withTenant("tenant2", () -> {
    // Operations run in tenant2 context
});

// Clear tenant context
tenantHelper.clearTenant();
```

### Test Data Builders

Create consistent test data:

```java
private Product createTestProduct(String sku) {
    Product product = new Product();
    product.setName("Test Product");
    product.setSku(sku);
    product.setBasePrice(new BigDecimal("99.99"));
    // ... set other fields
    return product;
}
```

## Continuous Integration

### GitHub Actions

The CI pipeline runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

CI stages:
1. **Test Matrix**: Tests with Java 17 and 21
2. **Unit Tests**: Fast feedback
3. **Integration Tests**: Full application testing
4. **Coverage Report**: Upload to Codecov
5. **Docker Build**: Verify containerization
6. **Security Scan**: Trivy vulnerability scanning
7. **Code Quality**: SpotBugs analysis

### Local CI Simulation

```bash
# Run the same tests as CI
./test.sh all

# Build Docker image like CI
docker build -t test-image .

# Run security scan
docker run --rm -v $(pwd):/app \
  aquasec/trivy:latest fs /app
```

## Troubleshooting Tests

### Common Issues

#### H2 Database Connection Issues
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Clean and retry
mvn clean test
```

#### Tenant Context Leakage
```java
@AfterEach
void cleanup() {
    TenantContext.clear();
}
```

#### Port Conflicts in Integration Tests
```yaml
# Use random port in application-test.yml
server:
  port: 0
```

### Memory Issues

For large test suites:
```bash
# Increase memory for Maven
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"

# Run tests with memory profiling
mvn test -Dspring.profiles.active=test -XX:+PrintGCDetails
```

### Debugging Tests

```bash
# Run specific test class
mvn test -Dtest=ProductServiceTest

# Run specific test method
mvn test -Dtest=ProductServiceTest#shouldCreateProduct

# Debug mode
mvn test -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

## Best Practices

### Test Naming
- Use descriptive test method names: `shouldCreateProductWhenValidDataProvided`
- Use `@DisplayName` for better test reports
- Group related tests in inner classes

### Test Structure
- Follow Given-When-Then pattern
- One assertion per test (when possible)
- Use AssertJ for fluent assertions

### Mock Usage
- Mock external dependencies only
- Prefer real objects for value objects
- Use `@MockBean` for Spring context mocks

### Test Data Management
- Create fresh test data per test
- Use builders for complex objects
- Avoid shared test state

### Performance
- Keep unit tests under 100ms each
- Use `@DirtiesContext` sparingly
- Profile slow tests with JProfiler

## Metrics and Reporting

### Test Execution Metrics
- Total test count: ~50+ tests
- Average execution time: <30 seconds
- Coverage target: 80%+

### Reports Generated
- JUnit XML reports: `target/surefire-reports/`
- Coverage reports: `target/site/jacoco/`
- Integration test reports: `target/failsafe-reports/`

### Quality Gates
- All tests must pass
- Coverage must be >80%
- No critical security vulnerabilities
- Build must complete successfully