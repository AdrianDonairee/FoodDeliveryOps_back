# FoodDeliveryOps Backend

FoodDeliveryOps Backend is the API service for the FoodDeliveryOps project. It is built with Spring Boot and connects to a PostgreSQL database for application data.

The project now includes a complete authentication slice backed by PostgreSQL, JWT access tokens, refresh tokens, and an admin-only account management API.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Validation
- Spring Data / JPA support through the project configuration
- PostgreSQL 16
- Maven

## Requirements

Before running the backend, make sure you have:

- Java 17 installed
- Maven installed, or use the included Maven wrapper
- Docker installed if you want to run PostgreSQL locally with `docker-compose`
- PostgreSQL available on `localhost:5432` if you prefer running it manually

## Project Structure

The main backend code lives under:

- `src/main/java/com/adriandonaire/fooddeliveryops/` for the application entry point and controllers
- `src/main/java/com/adriandonaire/fooddeliveryops/auth/` for authentication-related code
- `src/main/java/com/adriandonaire/fooddeliveryops/order/` for order-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/rider/` for rider-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/rotiseria/` for rotiseria-related logic
- `src/main/java/com/adriandonaire/fooddeliveryops/user/` for user-related logic
- `src/main/resources/application.yml` for application configuration

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

If those values are not provided, the application falls back to development defaults.

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

## Available Endpoints

The backend currently exposes these routes:

- `POST /auth/login` authenticates a username and password and returns access and refresh tokens
- `POST /auth/refresh` rotates a refresh token and returns a new session
- `POST /auth/logout` revokes a refresh token
- `GET /auth/me` returns a simple authenticated marker for the current session
- `GET /admin/users` lists all accounts, admin only
- `POST /admin/users` creates a new account with a unique username and role, admin only
- `GET /hello` returns a small JSON response for health-style checks

Example response:

```json
{
	"greating": "Hello World"
}
```

## Useful Commands

- `./mvnw test` to run the test suite
- `./mvnw clean package` to build the application
- `docker compose up -d` to start PostgreSQL
- `docker compose down` to stop the database container

On Windows, replace `./mvnw` with `mvnw.cmd`.

## Development Notes

- Hibernate is configured with `ddl-auto: update`, so the schema is updated automatically during development.
- The package structure is already separated by domain, which makes it easier to grow the API in feature-oriented modules.
- Authentication is stateless for access tokens and persistent for refresh tokens, so sessions can be revoked while the API remains easy to consume from Expo.

## Running the Full Stack

To work with the frontend and backend together:

1. Start PostgreSQL with Docker Compose in the backend folder.
2. Start the backend with `mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`.
3. Start the frontend Expo app from the frontend folder with `npm run start`.

## Troubleshooting

- If the backend fails to start, check whether PostgreSQL is running on `localhost:5432`.
- If you see authentication errors, verify the database name, username, and password in `application.yml`.
- If port `8080` is already in use, change the `server.port` value in the configuration file.

## License

Add a license here if the project needs one.
