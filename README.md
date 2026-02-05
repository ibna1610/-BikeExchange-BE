# Bike Exchange Backend

A Spring Boot RESTful API for the Bike Exchange platform.

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Project Structure

```
src/
├── main/
│   ├── java/com/bikeexchange/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST API controllers
│   │   ├── service/         # Business logic
│   │   ├── repository/      # Data access layer
│   │   ├── model/           # Entity models
│   │   └── BikeExchangeApplication.java
│   └── resources/
│       └── application.yml  # Application configuration
└── test/
    └── java/com/bikeexchange/  # Test classes
```

## Getting Started

### Build the Project
```bash
mvn clean install
```

### Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Endpoints

- **Health Check**: `GET /api/health`

### Database

The application uses H2 in-memory database by default.

**H2 Console**: `http://localhost:8080/h2-console`

Database URL: `jdbc:h2:mem:testdb`

## Configuration

Edit `src/main/resources/application.yml` to customize:
- Server port
- Database settings
- Logging levels

## Development

### Database Configuration (MySQL)

To switch to MySQL, uncomment the MySQL dependency in `pom.xml` and update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bike_exchange
    driverClassName: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

## Testing

```bash
mvn test
```

## Technologies

- Spring Boot 3.1.5
- Spring Data JPA
- H2 Database
- Lombok
- Maven

## License

This project is licensed under the MIT License.
