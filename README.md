# User Management Service

Enterprise-grade backend service for managing users and their roles within the Umbarry ecosystem.

## Features

*   **User Management**: CRUD operations for users.
*   **Keycloak Integration**: Automatic user provisioning in Keycloak for authentication.
*   **RBAC Authorization**: Fine-grained access control using roles retrieved from the application database.
*   **Data Visibility (JsonView)**: Selective field visibility based on user roles (Reporter, Operator, Developer).
*   **Event-Driven Architecture**: Asynchronous welcome email notification using RabbitMQ.
*   **Idempotent Processing**: Ensures emails are sent exactly once using a dedicated Notifications table.
*   **Database Migrations**: Managed via Flyway.
*   **Pagination**: RESTful pagination using custom HTTP headers (`X-Total-Count`, etc.).

## Technologies Used

*   **Java**: 17
*   **Spring Boot**: 4.0.3
*   **Spring Data JPA**: For database access.
*   **PostgreSQL**: Primary persistent storage.
*   **RabbitMQ**: Message broker for asynchronous events.
*   **Keycloak**: Identity and Access Management (IAM).
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
    ```
    
2. Navigate to the project directory
    ```bash
    cd user-management-service
    ```   

3. Start the infrastructure and application:
    ```bash
    cd scripts && sh run.sh
    ```

The service will be available at `http://localhost:8080`. Keycloak is automatically configured with the `umbarry` realm and required client.

### Default Admin User
An initial administrator user is automatically created in both the database (via Flyway) and Keycloak:
*   **Email**: `admin@example.com`
*   **Password**: `admin`
*   **Role**: `OWNER`

## Authentication & Authorization

### User Creation Flow
1.  An administrator (role `OWNER`) creates a user via `POST /v1/users`.
2.  The service automatically creates the user in **Keycloak** (using their email as username).
3.  A random 8-character temporary password is generated.
4.  The user receives an email containing his password.

### Email Testing

All emails (including passwords) are captured by **MailHog**.
View them at: `http://localhost:8025`

### Obtaining a JWT Token
To interact with the API, you must obtain a token from Keycloak:

```bash
curl --location 'http://localhost:8081/realms/umbarry/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=umbarry-app' \
--data-urlencode 'username=admin@example.com' \
--data-urlencode 'password=admin' \
--data-urlencode 'grant_type=password'
```

### Roles and Permissions
Authorization is managed by the application database. The following roles are supported:

| Role | Permissions | Data Visibility (JsonView)                                                                                           |
| :--- | :--- |:---------------------------------------------------------------------------------------------------------------------|
| **OWNER** | Full access: Create, Read, Update, Delete users. | Full access to all fields (Developer View).                                                                          |
| **MAINTAINER** | Can update users and change user status. | Full access to all fields.                                                                                           |
| **DEVELOPER** | Authenticated access. | Full access to all fields.                                                                                           |
| **OPERATOR** | Read-only access to user lists and details. | Can see all fields **except roles**.                                                                                 |
| **REPORTER** | Read-only access to user lists and details. | Minimum visibility: Can see ID, Username, Status, and Timestamps. **Hidden**: Roles, Tax Code, Name, Surname, Email. |

### Data Visibility Matrix

| Field | Reporter | Operator | Developer/Maintainer/Owner |
| :--- | :---: | :---: |:--------------------------:|
| `id` | ✅ | ✅ |             ✅              |
| `username` | ✅ | ✅ |             ✅              |
| `status` | ✅ | ✅ |             ✅              |
| `createdAt` | ✅ | ✅ |             ✅              |
| `updatedAt` | ✅ | ✅ |             ✅              |
| `email` | ❌ | ✅ |             ✅              |
| `taxCode` | ❌ | ✅ |             ✅              |
| `name` | ❌ | ✅ |             ✅              |
| `surname` | ❌ | ✅ |             ✅              |
| `roles` | ❌ | ❌ |             ✅              |

## API Documentation

*   **Swagger UI**: `http://localhost:8080/swagger-ui.html`
*   **OpenAPI Spec**: [openapi.yml](src/main/resources/openapi.yml)

## Running tests and generate coverage report

1.  Build and run the tests using docker:
    ```bash
    cd scripts
    sh tests.sh
    ```
After runnning the tests, a **coverage report** will be available at: ./jacoco-report/index.html
(generated using **jacoco**)
