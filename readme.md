from pathlib import Path

readme = r"""# Salon Management System

A **Spring Boot microservices-based Salon Management System** designed to manage users, salons, categories, salon services, bookings, payments, reviews, and notifications.

The project is organized as independently deployable services with **service discovery, API Gateway, synchronous inter-service communication, asynchronous messaging, authentication/authorization, and payment integrations**.

## Architecture

```text
                           ┌──────────────────────┐
                           │      Client          │
                           └──────────┬───────────┘
                                      │
                                      ▼
                           ┌──────────────────────┐
                           │     API Gateway      │
                           └──────────┬───────────┘
                                      │
                     ┌────────────────┼────────────────┐
                     │                │                │
                     ▼                ▼                ▼
               User Service     Salon Service    Category Service
                     │                │                │
                     │                └──────┬─────────┘
                     │                       ▼
                     │              Service Offering
                     │
                     ▼
               Booking Service ───────► Payment Service
                     │                       │
                     │                       │
                     ▼                       ▼
             Notification Service       Razorpay / Stripe
                     
                     ▲
                     │
              RabbitMQ messaging

              Eureka Server
              (Service Discovery)
```

## Microservices

| Service | Responsibility |
|---|---|
| **Eureka Server** | Service discovery and registration |
| **Gateway Service** | API Gateway and centralized security/error handling |
| **User Service** | User management, authentication, authorization and Keycloak integration |
| **Salon Service** | Salon creation, management and search |
| **Category Service** | Salon service categories |
| **Service Offering Service** | Services offered by salons |
| **Booking Service** | Booking creation, updates, status management and salon reports |
| **Payment Service** | Payment processing and payment status management |
| **Notification Service** | User/salon notifications |
| **Review Service** | Creating, updating, retrieving and deleting salon reviews |

## Key Features

### User & Authentication
- User registration and login
- JWT/token-based authentication through Keycloak
- Role-based authorization
- Refresh token support
- Keycloak user and role management

### Salon Management
- Create and update salons
- Delete salons
- Get salons
- Search salons by city
- Get salons owned by a user

### Category Management
- Create categories
- Update categories
- Get categories
- Search category by name
- Delete categories

### Service Offering Management
- Create salon service offerings
- Update service offerings
- Retrieve service offerings
- Retrieve services for a salon
- Retrieve multiple services by IDs

### Booking Management
- Create bookings
- Update booking status
- Get booking by ID
- Get bookings by customer
- Get bookings by salon
- Get bookings by salon and date
- Generate salon reports
- Communication with User, Salon, Category, Service Offering and Payment services

### Payment Management
- Payment creation and retrieval
- Razorpay integration
- Stripe integration
- Payment verification
- Payment strategy abstraction using the Strategy Pattern
- Asynchronous communication with Booking and Notification services

### Notifications
- Create notifications
- Retrieve user notifications
- Retrieve salon notifications
- Mark notifications as read
- RabbitMQ-based event consumption

### Reviews
- Create reviews
- Update reviews
- Delete reviews
- Get review by ID
- Get all reviews for a salon

## Technologies Used

### Backend
- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- Spring Cloud
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway
- OpenFeign

### Authentication & Authorization
- Keycloak
- JWT
- Role-based access control

### Messaging
- RabbitMQ
- Spring AMQP
- Jackson JSON message conversion

### Payments
- Razorpay
- Stripe

### Database
- PostgreSQL

### Build & Deployment
- Maven
- Jib
- Docker
- Docker Compose

### API Testing
- Postman

## Communication Between Services

The project uses two communication approaches:

### Synchronous Communication

Services communicate synchronously using **OpenFeign**.

For example:

```text
Booking Service
      │
      ├──► User Service
      ├──► Salon Service
      ├──► Category Service
      ├──► Service Offering Service
      └──► Payment Service
```

Feign clients are organized inside the individual services under their `service/client` packages.

### Asynchronous Communication

RabbitMQ is used for event-driven communication.

Example:

```text
Payment Service
      │
      │ Payment/Booking event
      ▼
   RabbitMQ
      │
      ├──────────────► Booking Service
      │
      └──────────────► Notification Service
```

This allows notification and booking-related processing to be decoupled from synchronous API calls.

## Design Patterns

### Strategy Pattern

The Payment Service uses the Strategy Pattern to support multiple payment providers.

```text
                  PaymentStrategy
                       ▲
              ┌────────┴────────┐
              │                 │
        RazorpayStrategy    StripeStrategy
              │                 │
              └────────┬────────┘
                       ▼
              PaymentStrategyResolver
```

Adding another payment provider can be done by introducing another strategy implementation instead of modifying the core payment flow.

### Layered Architecture

Individual services follow a layered structure:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

DTOs, mappers, exception handling and Feign clients are separated into their respective packages.

## Project Structure

```text
Salon Management System/
│
├── user_service/
├── category_service/
├── salon_service/
├── service-offering_service/
├── booking_service/
├── payment_service/
├── notification-service/
├── review-service/
├── gateway-service/
├── eureka-server/
│
├── docker-compose/
│   ├── default/
│   └── postgres/
│
├── postman/
│   ├── collections/
│   └── environments/
│
└── readme.md
```

## Prerequisites

Make sure the following are installed:

- Java
- Maven
- Docker
- Docker Compose
- PostgreSQL
- RabbitMQ
- Keycloak

For local development, ensure PostgreSQL, RabbitMQ and Keycloak are running before starting the dependent services.

## PostgreSQL Setup

If PostgreSQL is installed through Homebrew:

```bash
brew services start postgresql@14
```

Connect to PostgreSQL:

```bash
psql -U postgres
```

The repository also contains PostgreSQL initialization files under:

```text
docker-compose/postgres/init/
```

and:

```text
docker-compose/default/postgres/init/
```

Use the provided Docker Compose setup when you want PostgreSQL managed through Docker.

## Build All Spring Boot Services

From the project root:

```bash
for dir in user_service category_service salon_service service-offering_service payment_service booking_service notification-service review-service gateway-service eureka-server;
do
    echo "===== $dir ====="
    (cd "$dir" && mvn clean compile jib:build -Djib.from.platforms=linux/arm64) || exit 1
done
```

> The `linux/arm64` Jib platform option is useful when building on Apple Silicon/ARM64 machines.

If you are not building ARM64 images, remove:

```bash
-Djib.from.platforms=linux/arm64
```

## Run With Docker Compose

Docker Compose configurations are available under:

```text
docker-compose/
```

Start the required infrastructure/services using the appropriate Compose file for your environment.

Before starting the application, verify that:
- PostgreSQL is available
- RabbitMQ is available
- Keycloak is available
- Docker images have been built
- Service configuration points to the correct hostnames/ports

## API Testing

A Postman collection is included in:

```text
postman/collections/Salon Management System/
```

The collection contains requests for:

- Authentication
- Users
- Salons
- Categories
- Service offerings
- Bookings
- Payments
- Notifications
- Reviews
- Keycloak operations

Import the collection and environment into Postman to test the APIs.

## Important Service Dependencies

A typical startup/dependency flow is:

```text
Infrastructure
    │
    ├── PostgreSQL
    ├── RabbitMQ
    └── Keycloak
           │
           ▼
     Eureka Server
           │
           ▼
      API Gateway
           │
     ┌─────┴─────────────────────────────┐
     ▼                                   ▼
Business Services                  Supporting Services
     │                                   │
     ├── User Service                    ├── Payment Service
     ├── Salon Service                   ├── Notification Service
     ├── Category Service                └── Review Service
     ├── Service Offering
     └── Booking Service
```

## API Documentation / Postman

The repository contains a structured Postman collection covering the available APIs and their request definitions.

Main collection areas include:

- Auth Service
- User Service
- Salon Service
- Category Service
- Service Offering Service
- Booking Management Service
- Payment Service
- Notification Service
- Review Service
- Keycloak

## Error Handling

Services contain centralized exception handling with custom exceptions such as:

- `BadRequestException`
- `NotFoundException`
- `ForbiddenException`
- `GlobalException`

Common response DTOs are also maintained for success, error and validation responses.

## Development Notes

Each service is maintained as a separate Maven/Spring Boot application and contains its own:

- `pom.xml`
- Application entry point
- Controllers
- DTOs
- Entities
- Repositories
- Services
- Mappers
- Exceptions
- Tests

This structure allows individual services to be developed, built and deployed independently.

## Future Improvements

Potential improvements for a production deployment include:

- Centralized configuration management
- Distributed tracing
- Centralized logging
- Circuit breakers and retry policies
- API documentation with OpenAPI/Swagger
- Automated CI/CD pipeline
- Container orchestration
- Comprehensive integration and contract testing
- Secrets management

## Author

**Nikhil Singh**

Java / Spring Boot / Microservices Project
"""

path = "/mnt/data/README.md"
Path(path).write_text(readme, encoding="utf-8")
print(path)
