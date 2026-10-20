# =========================================================================
# STAGE 1: Build JAR with Maven (Builder Stage)
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copiar configuración de dependencias primero para aprovechar cache de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copiar código fuente y compilar artefacto JAR
COPY src ./src
RUN mvn clean package -DskipTests

# =========================================================================
# STAGE 2: Production Distroless/Minimal Runtime
# =========================================================================
FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

# Crear usuario sin privilegios por seguridad (12-Factor / Best Practices)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar únicamente el archivo JAR empaquetado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

# Healthcheck nativo para orquestadores y monitoreo de desechabilidad (12-Factor IX: Disposability)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Parámetros optimizados para contenedores (ajuste dinámico de memoria RAM en contenedores)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
