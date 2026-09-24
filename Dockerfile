FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 cinema
COPY --from=build /workspace/target/*.jar app.jar

USER cinema
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
