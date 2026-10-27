# FoodDeliveryOps Backend
//prueba
FoodDeliveryOps Backend is the central API and logistics engine for the **FoodDeliveryOps** platform. Built with **Spring Boot 4** (Java 17) and **PostgreSQL 16**, it manages role-based access, food shop order dispatching, background rider GPS tracking, real-time proximity-based rider assignment, and full order lifecycle tracking.

The project includes a complete authentication slice backed by PostgreSQL, JWT access tokens, refresh tokens, an admin-only account management API, an automated rider assignment engine, and background location ingest services.

---

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Validation
- Spring Data / JPA support through the project configuration
- PostgreSQL 16
- Maven

---

## Requirements

Before running the backend, make sure you have:

- Java 17 installed
- Maven installed, or use the included Maven wrapper
- Docker installed if you want to run PostgreSQL locally with `docker-compose`
- PostgreSQL available on `localhost:5432` if you prefer running it manually

---

## Key Features & Architecture

### 1. Role-Based Security & User Management (RBAC)
- **Strict Registration Policy**: No public user sign-ups. Accounts are exclusively created, updated, and managed by system Administrators.
- **Roles**:
  - `ROLE_ADMIN`: Access to user creation and system-wide account management.
  - `ROLE_FOOD_SHOP` (Food Shop): Access to order creation and food shop order monitoring.
  - `ROLE_RIDER`: Access to rider status toggles, incoming order acceptance/rejection, route info, and location pings.
- **Authentication**: JWT-based access tokens with persistent DB refresh tokens for session rotation and instant revocation.

### 2. Intelligent Rider Assignment Engine
- **Proximity Calculation**: Calculates real-time distance (Haversine / Spatial SQL queries) between the Food Shop location and active riders.
- **Rider Filtering**: Matches orders only to riders who meet all criteria:
  1. Duty Status = `TRABAJANDO` (On Duty)
  2. Availability Status = `LIBRE` (Free / Not currently on a delivery)
  3. Active GPS signal / connected
- **Sequential Re-assignment Queue**:
  - When an order is created, the nearest eligible rider receives an assignment request.
  - If the rider **Rejects** (or times out), the engine automatically assigns the order to the next closest free rider.
  - If all available riders reject, the order remains in `PENDIENTE` queue until a rider becomes available or accepts.

### 3. Background GPS Location Tracking
- Ingests periodic background location updates (latitude, longitude, timestamp) sent by active riders while in `TRABAJANDO` state.
- Persists rider coordinates to ensure up-to-second positioning for the assignment engine.

### 4. Order Lifecycle State Machine
Strict state transitions for every delivery:
- `PENDIENTE`: Order submitted by Food Shop; waiting for rider assignment/acceptance.
- `ASIGNADO`: Order accepted by a rider; rider availability changes to `OCUPADO`.
- `EN_CAMINO`: Rider picked up the food from the shop and is in transit to the customer.
- `ENTREGADO`: Order successfully handed to customer. Rider availability automatically resets to `LIBRE`.
- `CANCELADO`: Order cancelled by Food Shop or Admin prior to delivery.

---

## Project Structure

The main backend code lives under:

- `src/main/java/com/adriandonaire/fooddeliveryops/` for the application entry point and controllers
- `src/main/java/com/adriandonaire/fooddeliveryops/auth/` for authentication-related code
- `src/main/java/com/adriandonaire/fooddeliveryops/order/` for order-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/rider/` for rider-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/food_shop/` for food shop-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/user/` for user-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/logistics/` for proximity calculation and auto-assignment engine
- `src/main/resources/application.yml` for application configuration

---

## Database Setup

The backend is configured to use PostgreSQL with these default values:

- Database: `fooddeliveryops`
- Username: `foodops`
- Password: `foodops`
- Host: `localhost`
- Port: `5432`

You can start the database with Docker Compose from the backend folder:

```bash
docker compose up -d
```

If the container does not exist yet, that same command creates it and leaves it running in the background. After that, Docker will show it as `FoodDeliveryOpsDB`.

If your environment uses the older Compose command, this also works:

```bash
docker-compose up -d
```

The backend also seeds an initial administrator account the first time it starts:

- Username: `admin`
- Password: `admin123`
- Role: `ROLE_ADMIN`

---

## Configuration

The current application configuration is defined in `src/main/resources/application.yml`.

```yaml
spring:
	datasource:
		url: jdbc:postgresql://localhost:5432/fooddeliveryops
		username: foodops
		password: foodops
	jpa:
		hibernate:
			ddl-auto: update
server:
	port: 8080
```

If you need to connect to a different database, update these values before starting the app.

The authentication layer also uses these optional settings:

- `app.jwt.secret` to sign access tokens
- `app.jwt.access-token-minutes` to control access token lifetime
- `app.jwt.refresh-token-days` to control refresh token lifetime
- `app.assignment.max-search-radius-km` to control maximum rider search radius

If those values are not provided, the application falls back to development defaults.

---

## Setup

From the backend project directory, install dependencies and prepare the app with Maven:

```bash
./mvnw clean install
```

On Windows, you can use:

```powershell
mvnw.cmd clean install
```

If the wrapper is not available or you prefer a global Maven installation, use:

```bash
mvn clean install
```

---

## Run the Backend

Start the API with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

If you already built the project, you can also run the generated JAR:

```bash
java -jar target/FoodDeliveryOps-api-0.0.1-SNAPSHOT.jar
```

The API will run on port `8080` by default.

---

## Available Endpoints

The backend exposes these routes:

### 🔐 Authentication & Health (`/auth`, `/hello`)
- `POST /auth/login` authenticates a username and password and returns access and refresh tokens
- `POST /auth/refresh` rotates a refresh token and returns a new session
- `POST /auth/logout` revokes a refresh token
- `GET /auth/me` returns a simple authenticated marker for the current session
- `GET /hello` returns a small JSON response for health-style checks:
  ```json
  {
  	"greating": "Hello World"
  }
  ```

### 🛠️ Admin User Management (`/admin/users`)
- `GET /admin/users` lists all accounts, admin only
- `POST /admin/users` creates a new account with a unique username, password, and role (`ADMIN`, `FOOD_SHOP`, `RIDER`), admin only
- `PUT /admin/users/{id}` updates user role or credentials, admin only

### 🍕 Food Shop (`/food_shop`)
- `POST /food_shop/orders` creates a new order (customer written address + lat/lng coordinates, items/total). Triggers automatic rider assignment.
- `GET /food_shop/orders` lists active and past orders created by this food shop
- `POST /food_shop/orders/{id}/cancel` cancels an unfulfilled order in `PENDIENTE` state

### 🛵 Rider Operations & Location (`/rider`)
- `PATCH /rider/status/duty` toggles working status (`TRABAJANDO` / `NO_TRABAJANDO`)
- `POST /rider/location` ingests background GPS position pings (`latitude`, `longitude`, timestamp)
- `GET /rider/assignments/current` fetches pending order offer or active order details
- `POST /rider/assignments/{orderId}/accept` accepts assigned order. Updates order to `ASIGNADO` & rider to `OCUPADO`
- `POST /rider/assignments/{orderId}/reject` rejects order offer. Triggers automatic re-assignment to next nearest rider
- `PATCH /rider/orders/{orderId}/status` advances lifecycle (`EN_CAMINO`, `ENTREGADO`). Resets rider to `LIBRE` on delivery

---

## Useful Commands

- `./mvnw test` to run the test suite
- `./mvnw clean package` to build the application
- `docker compose up -d` to start PostgreSQL
- `docker compose down` to stop the database container

On Windows, replace `./mvnw` with `mvnw.cmd`.

---

## Development Notes

- Hibernate is configured with `ddl-auto: update`, so the schema is updated automatically during development.
- The package structure is separated by domain, which makes it easier to grow the API in feature-oriented modules.
- Authentication is stateless for access tokens and persistent for refresh tokens, so sessions can be revoked while the API remains easy to consume from Expo.
- Rider location coordinates are sent in background and processed by the proximity engine to compute real distances for order assignment.

---

## Running the Full Stack

To work with the frontend and backend together:

1. Start PostgreSQL with Docker Compose in the backend folder.
2. Start the backend with `mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`.
3. Start the frontend Expo app from the frontend folder with `npm run start`.

---

## Troubleshooting

- If the backend fails to start, check whether PostgreSQL is running on `localhost:5432`.
- If you see authentication errors, verify the database name, username, and password in `application.yml`.
- If port `8080` is already in use, change the `server.port` value in the configuration file.
