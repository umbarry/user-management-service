# User Management Service

Enterprise-grade backend service for managing users and their roles within the Umbarry ecosystem.

## Features

*   **User Management**: CRUD operations for users.
*   **Role Management**: Support for multiple roles per user (OWNER, OPERATOR, MAINTAINER, DEVELOPER, REPORTER).
*   **Event-Driven Architecture**: Asynchronous welcome email notification using RabbitMQ.
*   **Idempotent Processing**: Ensures emails are sent exactly once using a dedicated Notifications table.
*   **Database Migrations**: Managed via Flyway.
*   **Pagination**: RESTful pagination using custom HTTP headers (`X-Total-Count`, etc.).

## Technologies Used

*   **Java**: 17
*   **Spring Boot**: 3.2.x
*   **Spring Data JPA**: For database access.
*   **PostgreSQL**: Primary persistent storage.
*   **RabbitMQ**: Message broker for asynchronous events.
*   **Flyway**: Database schema versioning.
*   **Lombok**: To reduce boilerplate code.
*   **MailHog**: For SMTP testing and email visualization.
*   **Docker**: For easy environment setup.

## Getting Started

### Prerequisites

*   Docker and Docker Compose

### Running the Application

1.  Clone the repository:
    ```bash
    git clone https://github.com/umbarry/user-management-service.git
    cd user-management-service
    ```

2.  Start the infrastructure and application:
    ```bash
    docker-compose up -d --build
    ```

The service will be available at `http://localhost:8080`.

## API Documentation

*   **Swagger UI**: `http://localhost:8080/swagger-ui.html`
*   **OpenAPI Spec**: [openapi.yml](src/main/resources/openapi.yml)

### Pagination Headers

List endpoints return a JSON array in the body and pagination metadata in headers:
*   `X-Total-Count`: Total number of records.
*   `X-Total-Pages`: Total number of pages.
*   `X-Page-Number`: Current page index (0-based).
*   `X-Page-Size`: Requested page size.

## Email Testing

All emails sent by the service are captured by **MailHog**.
You can view them at: `http://localhost:8025`

## Development

### Database Schema
The database schema is managed by Flyway. Migrations are located in `src/main/resources/db/migration`.

### Messaging
When a user is created, a `UserCreatedEvent` is published to the `user-exchange` with the routing key `user.created`.
A consumer listens on `user-created-queue` to send a welcome email.

### Idempotency logic
The `UserEventConsumer` checks the `notifications` table for a record with `user_id` and `type = 'WELCOME_EMAIL'` before sending. This prevents duplicate emails if a message is retried by RabbitMQ.
