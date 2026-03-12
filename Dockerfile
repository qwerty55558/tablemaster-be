# Build stage
FROM --platform=linux/amd64 eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Gradle wrapper와 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 캐싱을 위해 먼저 다운로드
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM --platform=linux/amd64 eclipse-temurin:25-jre

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
