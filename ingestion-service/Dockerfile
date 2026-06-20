FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

RUN mvn -pl ingestion-service -am clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache git

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

RUN mkdir -p /tmp/coderag && chown appuser:appgroup /tmp/coderag

USER appuser

COPY --from=builder /app/ingestion-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.io.tmpdir=/tmp/coderag", \
  "-jar", "app.jar"]