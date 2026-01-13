# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Gradle Wrapper 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐시 레이어 (변경 빈도 낮음)
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# JAR 파일 추출 (layered jar)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# ============================================
# Stage 2: Runtime (Production)
# ============================================
FROM eclipse-temurin:25-jre-alpine AS runtime

# 보안: non-root 사용자 생성
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

WORKDIR /app

# Layered JAR 복사 (캐시 최적화)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# 소유권 변경
RUN chown -R appuser:appgroup /app

# non-root 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -Djava.security.egd=file:/dev/./urandom"

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
