# Banking System

A **Spring Boot microservices-based Banking System** designed to manage accounts, payments, transactions, fraud screening, and notifications.

The project is organized as independently deployable services with an **API Gateway, synchronous inter-service communication, asynchronous messaging, authentication/authorization, and payment integrations (Razorpay and Stripe)**.

## Architecture

```text
                           ┌──────────────────────┐
                           │       Client         │
                           └──────────┬───────────┘
                                      │
                                      ▼
                           ┌──────────────────────┐
                           │     API Gateway      │──────► Redis ⚠️ (rate limiting, if enabled)
                           └──────────┬───────────┘
                                      │
                     ┌────────────────┼────────────────┐
                     │                │                │
                     ▼                ▼                ▼
              Account Service   Payment Service   Transaction Service
                     ▲                │                │
                     │                │                ├──────► Fraud Detection Service
                     │                ▼                │
                     │        Razorpay / Stripe        │
                     │                                 │
                     └────────── Feign ────────────────┘
                     ▲
                     │ Feign (account lookup)
                     │
             Notification Service ◄──────── Kafka ⚠️ (topics)
                     │
                     ▼
               SMTP (Email)

        Shared: common-library (DTOs, responses, exceptions)
        Storage: PostgreSQL
```

## Microservices

| Service | Responsibility |
|---|---|
| **API Gateway** | Single entry point, request routing, centralized security/filters |
| **Account Service** | Account creation, lookup, balance, blocking, credit and debit |
| **Payment Service** | Payment processing and status management via Razorpay and Stripe |
| **Transaction Service** | Transaction creation, lifecycle, status and history |
| **Fraud Detection Service** | Rule-based screening of transactions |
| **Notification Service** | OTP and credit email notifications |
| **Common Library** | Shared DTOs, response wrappers, exceptions and utilities |

## Key Features

### Account Management
- Create accounts
- Get account details
- Get account balance
- Block accounts
- Credit and debit operations

### Payment Management
- Payment creation and retrieval
- Razorpay integration
- Stripe integration
- Payment verification / webhook handling ⚠️
- Payment provider abstraction using the **Strategy** and **Factory** patterns

### Transaction Management
- Create transactions
- Track transaction status
- Retrieve transaction history
- Fraud screening before processing ⚠️

### Fraud Detection
- Rule-based transaction screening ⚠️ (list your actual rules)

### Notifications
- OTP email notifications
- Credit email notifications
- Account lookup through a Feign client
- SMTP email delivery through `JavaMailSender`
- Strategy-based notification types

### Security
- Gateway-level request handling
- JWT-based authentication ⚠️ (remove if not implemented)

## Technologies Used

### Backend
- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- Spring Cloud Gateway
- OpenFeign
- Bean Validation
- Lombok

### Messaging
- Apache Kafka ⚠️

### Caching / Rate Limiting
- Redis ⚠️

### Payments
- Razorpay
- Stripe

### Email
- Spring Mail (`JavaMailSender`)

### Database
- PostgreSQL

### Build & Deployment
- Maven
- Jib
- Docker
- Docker Compose

## Communication Between Services

### Synchronous Communication

Services communicate synchronously using **OpenFeign**.

```text
Notification Service
      │
      └──► Account Service   (fetch account/recipient details)

Transaction Service ⚠️
      │
      ├──► Account Service   (debit / credit)
      └──► Fraud Detection Service   (screening)
```

### Asynchronous Communication

Kafka is used for event-driven communication ⚠️ (confirm topics and producers/consumers).

```text
Payment / Transaction Service
      │
      │ Event
      ▼
    Kafka
      │
      └──────────────► Notification Service
                              │
                              ▼
                        Email (SMTP)
```

This keeps notification delivery decoupled from the synchronous API call path.

## Design Patterns

### Strategy Pattern (Payments)

The Payment Service supports multiple providers behind a single interface.

```text
                  PaymentProvider (interface)
                          ▲
                 ┌────────┴────────┐
                 │                 │
         RazorpayProvider     StripeProvider
                 │                 │
                 └────────┬────────┘
                          ▼
                PaymentProviderFactory
```

### Factory Pattern (Payments)

`PaymentProviderFactory` selects the correct provider at runtime from the requested provider type (`RAZORPAY` / `STRIPE`). Spring injects all `PaymentProvider` beans and the factory maps them by type.

```java
public enum PaymentProviderType { RAZORPAY, STRIPE }

public interface PaymentProvider {
    PaymentProviderType getType();
    PaymentResponse createPayment(PaymentRequest request);
}

@Component
public class PaymentProviderFactory {
    private final Map<PaymentProviderType, PaymentProvider> providers;

    public PaymentProviderFactory(List<PaymentProvider> list) {
        this.providers = list.stream()
            .collect(Collectors.toMap(PaymentProvider::getType, Function.identity()));
    }

    public PaymentProvider getProvider(PaymentProviderType type) {
        return Optional.ofNullable(providers.get(type))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + type));
    }
}
```

> ⚠️ Align class and method names with your actual code.

Adding another provider means adding one new `PaymentProvider` implementation and one enum value. The core payment flow doesn't change.

### Strategy + Factory Pattern (Notifications)

```text
                NotificationService (interface)
                          ▲
                 ┌────────┴──────────┐
                 │                   │
        OtpEmailNotification   CreditEmailNotification
                 │                   │
                 └────────┬──────────┘
                          ▼
                 NotificationFactory  ◄── NotificationTypes (enum)
                          │
                          ▼
                 EmailSender (interface)
                          ▲
                          │
                 SMTP implementation (JavaMailSender)
```

| Component | Role |
|---|---|
| `NotificationService` | Strategy interface |
| `OtpEmailNotification` | OTP email strategy |
| `CreditEmailNotification` | Credit-alert email strategy |
| `NotificationTypes` | Enum used to choose the strategy |
| `EmailSender` | Email delivery abstraction |
| `AccountServiceClient` | Feign client for account lookup |
| `FeignClientResponseMapper` | Maps Feign responses to notification data |

### Layered Architecture

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

DTOs, mappers, exception handling and Feign clients live in their own packages.

## Project Structure

```text
Banking System/
│
├── account-service/
├── payment-service/
├── transaction-service/
├── fraud-detection-service/
├── notification-service/
├── api-gateway/
├── common-library/
│
├── docker-compose/
│
├── .gitignore
└── README.md
```

> There is no root `pom.xml`. Each module is built independently, and `common-library` must be installed first.

## Prerequisites

- Java (⚠️ version, e.g. 17 or 21)
- Maven
- Docker and Docker Compose
- PostgreSQL
- Kafka ⚠️
- Redis ⚠️
- A Razorpay test account
- A Stripe test account
- An SMTP account (for example a Gmail app password)

## Environment Variables

Never commit real secrets. Export them locally or use an untracked `.env` file.

```bash
# Email
export MAIL_USERNAME="your-email@example.com"
export MAIL_PASSWORD="your-app-password"

# Razorpay  ⚠️ confirm property names
export RAZORPAY_KEY_ID="rzp_test_xxxxxxxx"
export RAZORPAY_KEY_SECRET="your-razorpay-secret"

# Stripe  ⚠️ confirm property names
export STRIPE_SECRET_KEY="sk_test_xxxxxxxx"
export STRIPE_WEBHOOK_SECRET="whsec_xxxxxxxx"
```

> If any real key was ever committed to Git, rotate it. Deleting it from the latest commit does not remove it from history.

## PostgreSQL Setup

If PostgreSQL is installed through Homebrew:

```bash
brew services start postgresql@14
psql -U postgres
```

Or start it through Docker Compose from the `docker-compose/` directory ⚠️ (confirm file names and any `init` scripts).

## Build All Spring Boot Services

Install the shared library first:

```bash
cd common-library && mvn clean install && cd ..
```

Then build and push each service image from the project root:

```bash
for dir in account-service payment-service transaction-service fraud-detection-service notification-service api-gateway;
do
    echo "===== $dir ====="
    (cd "$dir" && mvn clean compile jib:build -Djib.from.platforms=linux/arm64) || exit 1
done
```

> The `linux/arm64` option is for Apple Silicon. Remove `-Djib.from.platforms=linux/arm64` if you are not building ARM64 images.
> ⚠️ `jib:build` pushes to a registry. Use `jib:dockerBuild` to build to your local Docker daemon instead.

## Run With Docker Compose

```bash
cd docker-compose
docker compose up -d
docker compose ps
docker compose logs -f
```

Before starting the application, verify that:
- PostgreSQL is available
- Kafka is available ⚠️
- Redis is available ⚠️
- Docker images have been built
- Service configuration points to the correct hostnames and ports (use service names inside containers, not `localhost`)

> ⚠️ **Warning:** `docker compose down -v` deletes volumes and permanently erases database data.

## Run Services Locally (without Docker)

```bash
cd account-service && mvn spring-boot:run
cd fraud-detection-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

## Important Service Dependencies

```text
Infrastructure
    │
    ├── PostgreSQL
    ├── Kafka ⚠️
    └── Redis ⚠️
           │
           ▼
     Account Service
           │
     ┌─────┴─────────────────┐
     ▼                       ▼
Fraud Detection         Payment Service
     │                       │
     └──────────┬────────────┘
                ▼
       Transaction Service
                │
                ▼
       Notification Service
                │
                ▼
          API Gateway
```

## API Testing

> ⚠️ Add your Postman collection under `postman/` and update this section, or delete it.

Main areas to cover:

- Accounts
- Payments (Razorpay and Stripe)
- Transactions
- Fraud screening
- Notifications

Example through the gateway:

```bash
curl -X POST http://localhost:<GATEWAY_PORT>/<payments-path> \
  -H "Content-Type: application/json" \
  -d '{ "amount": 500.00, "currency": "INR", "provider": "RAZORPAY" }'
```

## Error Handling

Services use centralized exception handling with custom exceptions and common response DTOs from `common-library` for success, error and validation responses. ⚠️ List the exact exception classes you have.

## Development Notes

Each service is a separate Maven / Spring Boot application with its own:

- `pom.xml`
- Application entry point
- Controllers
- DTOs
- Entities
- Repositories
- Services
- Exceptions
- Tests

This allows each service to be developed, built and deployed independently.

## Future Improvements

- Idempotency keys for payments and transfers
- Saga / compensating actions for failed transactions
- Retry and dead-letter handling for Kafka consumers
- Optimistic or pessimistic locking on balance updates
- Distributed tracing and centralized logging
- Circuit breakers and retry policies
- OpenAPI / Swagger documentation
- Secrets management
- Stronger fraud detection rules
- Comprehensive integration and contract testing

## Author

**Your Name**

Java / Spring Boot / Microservices Project