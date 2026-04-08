# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar Maven wrapper e pom.xml (cache de dependências)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar código-fonte e compilar
COPY src src
COPY drugs-cache.json .
RUN ./mvnw clean package -DskipTests -B

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar JAR do stage de build
COPY --from=build /app/target/pharmaai-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/drugs-cache.json .

# Cloud Run usa a variável PORT (default 8080)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
