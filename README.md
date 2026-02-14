# Bus Ticket Booking System

A comprehensive bus ticket booking website that enables users to search for bus routes, select seats visually, book tickets, and make payments.

## Technology Stack

- **Backend**: Java 17 with Spring Boot 3.2.1
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, jqwik (property-based testing)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 15+
- Redis 7+

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database for development:

```bash
createdb bus_ticket_dev
```

Or using psql:

```sql
CREATE DATABASE bus_ticket_dev;
```

### 2. Redis Setup

Start Redis server:

```bash
redis-server
```

Or using Docker:

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

### 3. Configuration

Update database credentials in `src/main/resources/application-dev.properties` if needed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bus_ticket_dev
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Project Structure

```
src/
├── main/
│   ├── java/com/busticket/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST API controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Custom exceptions
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # Security configuration
│   │   └── service/         # Business logic services
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-staging.properties
│       ├── application-prod.properties
│       └── logback-spring.xml
└── test/
    ├── java/com/busticket/  # Test classes
    └── resources/
        └── application-test.properties
```

## Running Tests

```bash
mvn test
```

## Environment Profiles

- **dev**: Development environment (default)
- **staging**: Staging environment
- **prod**: Production environment

To run with a specific profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=staging
```

## Next Steps

1. Run database migrations (Task 2)
2. Implement domain models (Task 3)
3. Implement authentication (Task 4)
4. Implement search functionality (Task 5)

## License

Proprietary
