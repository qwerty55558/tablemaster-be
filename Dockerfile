# ============================================
# TableMaster Backend - AMD64 Dockerfile
# ============================================
# Build: docker build -t tablemaster-be .
# Run:   docker run -p 8080:8080 tablemaster-be
# ============================================

# Stage 1: Build
FROM --platform=linux/amd64 eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper 복사
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐싱을 위해 build 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./

# 소스 코드 복사
COPY src src

# 빌드 (테스트 스킵)
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:25-jre-alpine

WORKDIR /app

# 보안: non-root 사용자 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appgroup /app

USER appuser

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
