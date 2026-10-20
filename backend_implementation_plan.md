

---

## 🌿 Git Branching Strategy & Workflow

To maintain a clean and reliable codebase, the development follow a modified **GitFlow** approach:

*   **`main` / `master`**: Production-ready code. Always stable and deployable.
*   **`develop`**: Integration branch for active feature development.
*   **Feature Branches (`feature/<issue-id>-<short-description>`)**: Branched off `develop`. Used for implementing specific issues.
*   **Fix / Hotfix Branches (`fix/<issue-id>-<short-description>`)**: Branched off `develop` (or `main` for critical production bugs).

### Workflow Rules:
1. Every task must be associated with an Issue number.
2. Code must pass unit/integration tests (`./mvnw test`) before merging.
3. Feature branches merge into `develop` via Pull/Merge Requests.

---

## 🚀 Branches & Issue Breakdown

```
develop
 ├── feature/01-project-setup-db
 ├── feature/02-auth-rbac
 ├── feature/03-admin-user-management
 ├── feature/04-food-shop-order-management
 ├── feature/05-rider-status-location-ingest
 ├── feature/06-proximity-assignment-engine
 └── feature/07-order-lifecycle-state-machine
```

---

### Branch: `feature/01-project-setup-db`
**Objective:** Initialize the Spring Boot 4 application structure, configure PostgreSQL 16 connection, Docker Compose, and base JPA configuration.

#### 📋 Issue #1.1: Project Initialization & Maven Setup
*   **Title:** Initial Spring Boot 4 project baseline and Maven configuration
*   **Description:**
    *   Initialize Spring Boot 4 project with Java 17.
    *   Include dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `lombok`.
    *   Configure `mvnw` wrapper and test basic compile task `./mvnw clean install`.

#### 📋 Issue #1.2: Docker Compose & PostgreSQL 16 Integration
*   **Title:** Set up PostgreSQL Docker Compose environment and `application.yml`
*   **Description:**
    *   Create `docker-compose.yml` defining the `FoodDeliveryOpsDB` container on port `5432` with database `fooddeliveryops`, user `foodops`, and password `foodops`.
    *   Configure `src/main/resources/application.yml` with JDBC database properties and Hibernate `ddl-auto: update`.
    *   Create a basic `/hello` health check endpoint returning `{"greating": "Hello World"}`.

---

### Branch: `feature/02-auth-rbac`
**Objective:** Implement JWT-based stateless authentication, persistent DB refresh tokens, and Spring Security role-based access control (RBAC).

#### 📋 Issue #2.1: User Entity & Role Hierarchy Model
*   **Title:** Create User domain model and Role enum
*   **Description:**
    *   Define `User` entity (`id`, `username`, `password`, `role`, `createdAt`, `updatedAt`).
    *   Define `Role` enum: `ROLE_ADMIN`, `ROLE_FOOD_SHOP`, `ROLE_RIDER`.
    *   Implement initial DB seeder logic to create default admin (`admin` / `admin123`) on system boot.

#### 📋 Issue #2.2: JWT Access & Refresh Token Architecture
*   **Title:** Implement JWT generation, verification, and refresh token storage
*   **Description:**
    *   Implement `JwtService` using `app.jwt.secret`, `app.jwt.access-token-minutes`, and `app.jwt.refresh-token-days`.
    *   Create `RefreshToken` entity for DB-backed session tracking and revocation.
    *   Implement Spring Security filter chain for JWT authentication.

#### 📋 Issue #2.3: Authentication Endpoints
*   **Title:** Expose `/auth/login`, `/auth/refresh`, `/auth/logout`, and `/auth/me`
*   **Description:**
    *   `POST /auth/login`: Authenticate credentials, issue access & refresh tokens.
    *   `POST /auth/refresh`: Validate refresh token, issue new access token.
    *   `POST /auth/logout`: Revoke/delete active refresh token in database.
    *   `GET /auth/me`: Return authenticated session identity.

---

### Branch: `feature/03-admin-user-management`
**Objective:** Provide secure admin-only CRUD endpoints for user accounts enforcing the "No Public Self-Registration" policy.

#### 📋 Issue #3.1: Admin Account Management Endpoints
*   **Title:** Implement `/admin/users` endpoints for account creation and administration
*   **Description:**
    *   Enforce `ROLE_ADMIN` guard on all `/admin/**` endpoints.
    *   `GET /admin/users`: Retrieve paginated/listed account list.
    *   `POST /admin/users`: Admin creation of new accounts (username, password, role). Prevent duplicate usernames.
    *   `PUT /admin/users/{id}`: Update user credentials or role assignment.

---

### Branch: `feature/04-food-shop-order-management`
**Objective:** Implement order creation and monitoring endpoints for Food Shop operators.

#### 📋 Issue #4.1: Order Domain Model & Persistence
*   **Title:** Create Order entity and repository structure
*   **Description:**
    *   Define `Order` entity (`id`, `foodShopId`, `customerAddress`, `customerLat`, `customerLng`, `items`, `totalPrice`, `status`, `assignedRiderId`).
    *   Define `OrderStatus` enum: `PENDIENTE`, `ASIGNADO`, `EN_CAMINO`, `ENTREGADO`, `CANCELADO`.

#### 📋 Issue #4.2: Food Shop Order Controller & Lifecycle Actions
*   **Title:** Expose `/food_shop/orders` management routes
*   **Description:**
    *   Secure endpoints with `ROLE_FOOD_SHOP`.
    *   `POST /food_shop/orders`: Create new delivery order with GPS pin and customer info. Trigger assignment logic event.
    *   `GET /food_shop/orders`: Fetch active and historical orders belonging to the authenticated food shop.
    *   `POST /food_shop/orders/{id}/cancel`: Cancel an unfulfilled order in `PENDIENTE` status.

---

### Branch: `feature/05-rider-status-location-ingest`
**Objective:** Support rider duty/availability state toggles and high-frequency GPS ping ingestion.

#### 📋 Issue #5.1: Rider Profile & Status Management
*   **Title:** Implement rider duty and availability state persistence
*   **Description:**
    *   Define `RiderStatus` entity/fields: Duty Status (`TRABAJANDO` / `NO_TRABAJANDO`) and Availability Status (`LIBRE` / `OCUPADO`).
    *   `PATCH /rider/status/duty`: Allow rider to toggle working status.

#### 📋 Issue #5.2: GPS Location Ingest Service
*   **Title:** Implement `/rider/location` payload processing and spatial persistence
*   **Description:**
    *   `POST /rider/location`: Ingest `latitude`, `longitude`, and timestamp from riders in `TRABAJANDO` state.
    *   Store or update rider's latest known coordinates in PostgreSQL for quick proximity calculation.

---

### Branch: `feature/06-proximity-assignment-engine` [COMPLETED]
**Objective:** Build the core logistics engine calculating spatial proximity between food shops and riders, handling sequential assignment queues.

#### ✅ Issue #6.1: Spatial Proximity & Haversine Distance Calculator
*   **Title:** Develop proximity calculation service and rider filtering pipeline
*   **Description:**
    *   Implement Haversine formula / Spatial SQL queries to calculate real-time distance between Food Shop coordinates and active riders.
    *   Filter riders by: Duty = `TRABAJANDO`, Availability = `LIBRE`, Active recent GPS signal, within `app.assignment.max-search-radius-km`.

#### ✅ Issue #6.2: Sequential Re-assignment Queue & Response Handling
*   **Title:** Implement order offer routing, accept/reject handlers, and queue fallback
*   **Description:**
    *   `GET /rider/assignments/current`: Expose current assignment offer or active delivery to rider.
    *   `POST /rider/assignments/{orderId}/accept`: Set order to `ASIGNADO`, set rider to `OCUPADO`.
    *   `POST /rider/assignments/{orderId}/reject`: Automatically route order offer to the next closest eligible rider. If none remain, leave order in `PENDIENTE`.

---

### Branch: `feature/07-order-lifecycle-state-machine` [COMPLETED]
**Objective:** Finalize order progression transitions and ensure strict state validation.

#### ✅ Issue #7.1: Order Delivery Lifecycle Progression
*   **Title:** Implement `/rider/orders/{orderId}/status` state machine transition logic
*   **Description:**
    *   `PATCH /rider/orders/{orderId}/status`: Allow rider to advance status (`EN_CAMINO` upon food pickup, `ENTREGADO` upon customer delivery).
    *   When status transitions to `ENTREGADO`, automatically reset the rider's availability status to `LIBRE`.
    *   Validate all illegal state transitions (e.g., direct move from `PENDIENTE` to `ENTREGADO`).
