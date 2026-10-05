# Stage 1: build the React frontend (output goes to backend/src/main/resources/static)
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: build the Spring Boot (Kotlin) jar, with the website baked in
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /app/backend/src/main/resources/static ./src/main/resources/static
RUN mvn -q clean package -DskipTests

# Stage 3: small runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/backend/target/examguard.jar app.jar
ENV TZ=Asia/Kolkata
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
