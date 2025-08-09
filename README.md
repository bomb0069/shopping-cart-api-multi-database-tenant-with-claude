# Multi-Tenant Shopping Cart Platform

A Spring Boot application that provides a multi-tenant shopping cart platform where each tenant has separate databases for managing their products, prices, promotions, and shopping carts.

## Features

- **Multi-tenant Architecture**: Each tenant has its own isolated database
- **Product Management**: CRUD operations for products with categories, brands, and inventory
- **Dynamic Pricing**: Support for multiple price tiers, customer groups, and time-based pricing
- **Promotion System**: Flexible promotion engine with percentage, fixed amount, and buy-X-get-Y discounts
- **Shopping Cart**: Session-based and user-based cart management with promotion application
- **Tenant Resolution**: Support for tenant identification via headers, subdomains, and URL paths
- **REST APIs**: Complete RESTful API for all functionalities

## Architecture

### Multi-Tenant Database Strategy
- **Database per Tenant**: Each tenant has a completely separate database
- **Tenant Context**: Thread-local tenant context for isolation
- **Dynamic Data Source Routing**: Runtime data source selection based on tenant context

### Tenant Resolution
The application supports multiple ways to identify tenants:
1. **Header-based**: `X-Tenant-ID` header
2. **Subdomain-based**: `tenant1.example.com`
3. **Path-based**: `/tenant/tenant1/api/products`

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose (for containerized deployment)

### Quick Start with Docker (Recommended)

1. Clone the repository
2. Navigate to the project directory
3. Start the application with Docker:
   ```bash
   # Start all services (app + databases + nginx)
   ./start.sh
   # OR
   make start
   ```

The application will be available at:
- Application: `http://localhost:8080`
- Nginx Proxy: `http://localhost:80`
- PhpMyAdmin: `http://localhost:8081`

### Development Setup

For local development with external databases:
```bash
# Start only databases
./dev.sh
# OR
make dev

# Run application locally
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### Traditional Setup (Without Docker)

1. Run the application with H2 in-memory databases:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

### Testing Different Tenants

#### Using Headers
```bash
# Tenant 1
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/products

# Tenant 2
curl -H "X-Tenant-ID: tenant2" http://localhost:8080/api/products
```

#### Using Path-based Resolution
```bash
# Tenant 1
curl http://localhost:8080/tenant/tenant1/api/products

# Tenant 2
curl http://localhost:8080/tenant/tenant2/api/products
```

## API Endpoints

### Products
- `GET /api/products` - Get all active products (paginated)
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/sku/{sku}` - Get product by SKU
- `GET /api/products/category/{category}` - Get products by category
- `GET /api/products/search?q={term}` - Search products
- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

### Prices
- `GET /api/prices/product/{productId}/effective` - Get effective price for product
- `GET /api/prices/product/{productId}` - Get all prices for product
- `POST /api/prices` - Create new price rule
- `PUT /api/prices/{id}` - Update price rule

### Promotions
- `GET /api/promotions/active` - Get active promotions
- `GET /api/promotions/code/{code}` - Get promotion by code
- `POST /api/promotions/{id}/calculate-discount` - Calculate discount for order
- `POST /api/promotions` - Create new promotion
- `PUT /api/promotions/{id}` - Update promotion

### Shopping Cart
- `GET /api/cart` - Get current cart
- `POST /api/cart/items` - Add item to cart
- `PUT /api/cart/items` - Update cart item quantity
- `DELETE /api/cart/items/{productId}` - Remove item from cart
- `POST /api/cart/promotions/{code}` - Apply promotion code
- `DELETE /api/cart/promotions` - Remove applied promotion
- `DELETE /api/cart/clear` - Clear cart

## Database Configuration

The application uses H2 in-memory databases for demonstration. Each tenant gets its own database:

- `default`: Default tenant database
- `tenant1`: Tenant 1 database
- `tenant2`: Tenant 2 database

## Sample Data

The application loads sample data on startup for `tenant1` and `tenant2`:
- Sample products (Laptop, Wireless Mouse)
- Special pricing rules
- Promotional codes

## Configuration

Key configuration properties in `application.yml`:

```yaml
tenants:
  default:
    datasource:
      url: jdbc:h2:mem:default_db
  tenant1:
    datasource:
      url: jdbc:h2:mem:tenant1_db
  tenant2:
    datasource:
      url: jdbc:h2:mem:tenant2_db
```

## Security

Basic security configuration is included:
- CORS enabled for all origins
- CSRF disabled for API usage
- All endpoints are publicly accessible (suitable for demonstration)

## Technologies Used

- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security
- H2 Database
- Maven
- Java 17

## Development

### Adding New Tenants

1. Add datasource configuration in `application.yml`
2. Add tenant ID to `TenantService.AVAILABLE_TENANTS`
3. Add datasource bean in `TenantDataSourceConfig`
4. Update routing datasource target datasources

### Extending the Model

The application follows a clean architecture:
- **Models**: JPA entities in `com.shoppingcart.multitenant.model`
- **Repositories**: Spring Data repositories in `com.shoppingcart.multitenant.repository`
- **Services**: Business logic in `com.shoppingcart.multitenant.service`
- **Controllers**: REST endpoints in `com.shoppingcart.multitenant.controller`

## Docker Deployment

### Services Overview

The Docker setup includes:
- **shopping-cart-app**: Spring Boot application
- **mysql-default**: Default tenant database
- **mysql-tenant1**: Tenant 1 database  
- **mysql-tenant2**: Tenant 2 database
- **nginx**: Reverse proxy and load balancer
- **redis**: Session management (optional)
- **phpmyadmin**: Database management interface

### Available Commands

```bash
# Production deployment
make start          # Start all services
make stop           # Stop all services
make clean          # Stop and remove volumes
make logs           # View logs
make health         # Check service health
make test-api       # Test API endpoints

# Development
make dev            # Start databases only
make dev-stop       # Stop development databases

# Database connections
make db-connect-default   # Connect to default DB
make db-connect-tenant1   # Connect to tenant1 DB
make db-connect-tenant2   # Connect to tenant2 DB
```

### Environment Variables

Key environment variables (see `.env` file):
```bash
MYSQL_ROOT_PASSWORD=rootpassword
SPRING_PROFILES_ACTIVE=docker
JAVA_OPTS=-Xms512m -Xmx1g
```

### Docker Compose Files

- `docker-compose.yml`: Full production setup
- `docker-compose.dev.yml`: Development databases only

### Nginx Configuration

The Nginx proxy provides:
- Load balancing
- Rate limiting (10 req/s per IP)
- Tenant-based routing
- Security headers
- SSL support (configurable)

Access patterns:
- `http://localhost/api/products` → Default tenant
- `http://localhost/tenant/tenant1/api/products` → Tenant 1

## Production Considerations

For production deployment, consider:
- Configure SSL certificates in Nginx
- Use external MySQL servers with persistent storage
- Implement proper authentication and authorization
- Add monitoring with Prometheus/Grafana
- Configure log aggregation (ELK stack)
- Set up CI/CD pipelines
- Implement database backup strategies
- Add health checks and alerting