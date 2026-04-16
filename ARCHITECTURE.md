# Architecture in Depth

This document outlines the key architectural decisions made in the User Management Service, along with the rationale and any associated trade-offs.

## 1. Application Framework: Spring Boot

**Decision:** The service is built using Spring Boot.

**Rationale:**
*   **Dependency Injection (DI):** Facilitates loose coupling between components (Controllers, Services, Repositories), making the code more modular, maintainable, and testable.
*   **Inversion of Control (IoC):** The Spring container manages the lifecycle of beans, allowing developers to focus on business logic rather than object instantiation.
*   **Autoconfiguration:** Significant reduction in boilerplate configuration, enabling fast prototyping and standardized project structures.
*   **Testing Support:** Spring Boot provides excellent support for various testing layers, including:
    *   **Unit Tests:** Testing individual components in isolation.
    *   **Integration Tests:** Testing the interaction between components and external systems (e.g., Database, RabbitMQ) using `@SpringBootTest` and Testcontainers.
    *   **Slices:** Targeted testing of specific layers (e.g., `@WebMvcTest` for controllers, `@DataJpaTest` for repositories).
*   **Ecosystem:** Access to a vast range of Spring projects (Data JPA, Security, Cloud) that simplify complex enterprise requirements.

## 2. Database Choice: PostgreSQL

**Decision:** PostgreSQL was chosen as the primary relational database.

**Rationale:**
*   **Robustness and Reliability:** PostgreSQL is known for its strong adherence to SQL standards, data integrity, and advanced features like ACID compliance, making it suitable for critical business data.
*   **Scalability:** It offers various scaling options (vertical, horizontal with replication) to handle growing data volumes and user loads.
*   **Open Source:** Being open-source, it avoids vendor lock-in and has a large, active community.

## 3. Database Schema Design

The database schema is designed to store user information and their associated roles, along with a mechanism for tracking asynchronous notifications.

### `users` Table

```sql
CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,
   username VARCHAR(255) NOT NULL,
   email VARCHAR(255) NOT NULL UNIQUE,
   tax_code VARCHAR(16) NOT NULL UNIQUE,
   name VARCHAR(255) NOT NULL,
   surname VARCHAR(255) NOT NULL,
   status SMALLINT NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE,
   CONSTRAINT allowed_statuses CHECK (status IN (1, 2, 3))
);
```

**Rationale:**
*   **`id` (BIGSERIAL PRIMARY KEY):** Auto-incrementing primary key for unique identification of users. `BIGSERIAL` is used to accommodate a large number of users.
*   **`username` (VARCHAR):** User's chosen identifier, allowing for flexibility beyond email.
*   **`email` (VARCHAR NOT NULL UNIQUE):** Essential for communication and used as a login identifier. Enforced uniqueness ensures each user has a distinct email.
*   **`tax_code` (VARCHAR(16) NOT NULL UNIQUE):** A unique identifier for legal/tax purposes, ensuring no two users share the same tax code.
*   **`name`, `surname` (VARCHAR):** Basic personal identification fields.
*   **`status` (SMALLINT NOT NULL):** Represents the current state of the user (e.g., ACTIVE, DISABLED, DELETED). Using `SMALLINT` with a `CHECK` constraint ensures data integrity and efficient storage for a limited set of statuses.
*   **`created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE):** Standard audit fields to track when a record was created and last modified, including timezone information for accuracy.
*   **`CONSTRAINT allowed_statuses`:** Ensures that only predefined status values are stored, maintaining data consistency.

### `user_roles` Table

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role SMALLINT NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT allowed_roles CHECK (role IN (1, 2, 3, 4, 5))
);
```

**Rationale:**
*   **Many-to-Many Relationship:** This table implements a many-to-many relationship between users and roles, allowing a user to have multiple roles and a role to be assigned to multiple users.
*   **`user_id` (BIGINT NOT NULL):** Foreign key referencing the `id` from the `users` table. `ON DELETE CASCADE` ensures that if a user is deleted, their role assignments are also removed.
*   **`role` (SMALLINT NOT NULL):** Stores the role identifier. Using `SMALLINT` with a `CHECK` constraint ensures data integrity and efficient storage for a limited set of roles.
*   **`PRIMARY KEY (user_id, role)`:** A composite primary key ensures that a user cannot be assigned the same role multiple times.
*   **`CONSTRAINT allowed_roles`:** Ensures that only predefined role values are stored.

### `notifications` Table

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Rationale:**
*   **Idempotent Email Sending:** This table is crucial for ensuring that asynchronous email notifications are sent exactly once. Before sending an email, a record is created here. The `sent` flag and `version` field (for optimistic locking) help prevent duplicate sends in case of retries or system failures.
*   **`user_id` (BIGINT NOT NULL):** Links the notification to a specific user.
*   **`type` (VARCHAR):** Describes the type of notification (e.g., "WELCOME_EMAIL", "PASSWORD_RESET").
*   **`sent` (BOOLEAN):** Flag indicating whether the notification has been successfully processed.
*   **`version` (BIGINT):** Used for optimistic locking to prevent race conditions when updating the `sent` status.

## 4. Indexing Strategy

Indexes are critical for optimizing database query performance.

*   **`idx_user_email` on `users(email)`:** Accelerates lookups by email, which is frequently used for authentication, user retrieval, and uniqueness checks.
*   **`idx_user_tax_code` on `users(tax_code)`:** Improves performance for queries involving the tax code, also used for uniqueness and specific user identification.
*   **`idx_notifications_user_id` on `notifications(user_id)`:** Speeds up retrieval of all notifications for a given user.
*   **`idx_notifications_user_type` on `notifications(user_id, type)`:** Optimizes queries that fetch notifications of a specific type for a particular user.
*   **`idx_notifications_sent` on `notifications(sent)`:** Enhances performance for queries that need to find pending (not sent) notifications.

## 5. Database Migrations with Flyway

**Decision:** Flyway is used for managing database schema evolution.

**Rationale:**
*   **Version Control:** Database changes are treated like application code, stored in versioned SQL scripts. This ensures that the database schema is always in a known state and can be recreated reliably.
*   **Automated Migrations:** Flyway automates the process of applying schema changes, reducing manual errors and ensuring consistency across different environments (development, staging, production).
*   **Collaboration:** Facilitates team collaboration on database changes by providing a clear history and mechanism for resolving conflicts.
*   **Rollback Capability:** While Flyway doesn't directly support rollbacks, the versioned scripts make it easier to understand and manually revert changes if necessary.

## 6. Asynchronous Communication with RabbitMQ

**Decision:** RabbitMQ is employed as a message broker for asynchronous communication, particularly for sending emails.

**Rationale:**
*   **Decoupling:** Separates the user creation process from the email sending process. The user service doesn't need to wait for the email to be sent, improving response times and overall system responsiveness.
*   **Resilience:** If the email service is temporarily unavailable, messages are queued in RabbitMQ and retried later, preventing data loss and ensuring eventual consistency.
*   **Scalability:** Allows for independent scaling of the user service and the email sending service. Multiple email consumers can process messages concurrently.
*   **Topic Subscription Pattern:** Using topics allows for flexible routing of messages. For example, different types of notifications (welcome, password reset) can be routed to different queues or services, enabling future features without modifying the core user service.

**Trade-offs:**
*   Introduces additional complexity with a message broker infrastructure.
*   Requires careful handling of message idempotency (addressed by the `notifications` table) to prevent duplicate processing.

## 7. Identity Provider: Keycloak

**Decision:** Keycloak is integrated as the Identity and Access Management (IAM) solution.

**Rationale:**
*   **Centralized Authentication:** Keycloak handles user authentication, single sign-on (SSO), and identity federation, offloading these concerns from the application.
*   **Security Features:** Provides robust security features like multi-factor authentication, brute-force detection, and secure token issuance (JWT).
*   **Standard Protocols:** Supports industry-standard protocols like OAuth 2.0 and OpenID Connect, ensuring interoperability.
*   **User Management:** Keycloak provides its own user management interface, simplifying the administration of user accounts.
*   **Automatic Provisioning:** Users created in the User Management Service are automatically provisioned in Keycloak, ensuring a consistent user base.

**Trade-offs:**
*   Adds an external dependency and requires configuration and maintenance of the Keycloak server.

## 8. Authorization with Spring Security

**Decision:** Spring Security is used for managing authorization within the application.

**Rationale:**
*   **Robust Framework:** Spring Security is a mature and widely adopted framework for securing Spring-based applications, offering comprehensive authorization capabilities.
*   **Role-Based Access Control (RBAC):** Leverages annotations like `@PreAuthorize` to define fine-grained access rules based on user roles (e.g., `hasRole('OWNER')`, `hasAnyRole('OPERATOR', 'REPORTER')`).
*   **Integration with Keycloak:** Seamlessly integrates with Keycloak's JWT tokens to extract user roles and claims for authorization decisions.
*   **Reduced Boilerplate:** Provides declarative security, reducing the amount of security-related code that needs to be written manually.

## 9. Data Visibility with JsonView

**Decision:** Jackson's `JsonView` is utilized to control data visibility based on user roles.

**Rationale:**
*   **Fine-grained Control:** Allows specific fields of a DTO (`UserResponse`) to be exposed or hidden depending on the authenticated user's role (e.g., Reporter, Operator, Developer).
*   **Security and Compliance:** Ensures that sensitive information (like `taxCode` or `roles`) is only visible to authorized personnel, adhering to security and privacy requirements.
*   **Reduced Payload Size:** Prevents unnecessary data from being sent over the network, optimizing bandwidth and potentially improving performance for clients that don't need all fields.
*   **Single DTO:** Avoids the need to create multiple DTOs for different views of the same resource, simplifying maintenance.

## 10. Containerization with Docker

**Decision:** The application and its dependencies are containerized using Docker and managed with Docker Compose.

**Rationale:**
*   **Environment Consistency:** Ensures that the application runs consistently across different environments (development, testing, production) by packaging it with all its dependencies.
*   **Simplified Deployment:** Docker images can be easily deployed to any Docker-compatible environment, streamlining the deployment process.
*   **Isolation:** Containers provide process isolation, preventing conflicts between different applications or services running on the same host.
*   **Local Development:** Docker Compose simplifies the setup of the entire development environment, including PostgreSQL, RabbitMQ, and Keycloak, making it easy for new developers to get started.
