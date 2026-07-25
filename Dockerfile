FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S wallet && adduser -S wallet -G wallet
WORKDIR /app

COPY --from=build /workspace/target/wallet-service-*.jar app.jar

USER wallet
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
