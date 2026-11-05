# ==========================================
# ETAPA 1: Construcción (Build) con Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar el pom.xml y descargar dependencias primero para aprovechar cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Runtime ligero
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear directorio para la base de datos H2
RUN mkdir -p /app/data

# Copiar el archivo JAR generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Render inyecta la variable PORT automáticamente (por defecto 8080 si es local)
ENV PORT=8080
EXPOSE 8080

# Iniciar la aplicación Spring Boot
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
