# Spring Profile 설정 가이드

## 파일 구조

| 파일 | 용도 |
| --- | --- |
| `application.yaml` | 공통 설정 (포트, JWT 만료시간, RabbitMQ 등) |
| `application-dev.yaml` | 개발 환경 (localhost, ddl-auto: update) |
| `application-prod.yaml` | 상품 환경 (환경변수, ddl-auto: validate) |

---

## Dev vs Prod 비교

| 항목 | Dev | Prod |
| --- | --- | --- |
| DB URL | `localhost:5432` | 환경변수 (`SPRING_DATASOURCE_URL`) |
| Redis | `localhost:6379` | 환경변수 (`REDIS_HOST`) |
| RabbitMQ | `localhost:5672` | 환경변수 (`RABBITMQ_HOST`) |
| ddl-auto | `update` | `validate` |
| show-sql | `true` | `false` |
| 로그 레벨 | `DEBUG` | `INFO` / `WARN` |
| JWT Secret | 하드코딩 (개발용) | 환경변수 필수 |
| HikariCP Pool | max `5` | max `20` |

---

## 실행 방법

### 개발 환경 (기본값)

```bash
./gradlew bootRun
```

### 상품 환경

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

또는

```bash
export SPRING_PROFILES_ACTIVE=prod
./gradlew bootRun
```

---

## 상품 환경 필수 환경변수

| 변수명 | 설명 | 예시 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | DB 접속 URL | `jdbc:postgresql://db:5432/tablemaster` |
| `POSTGRES_USER` | DB 사용자 | `postgres` |
| `POSTGRES_PASSWORD` | DB 비밀번호 | `secret` |
| `REDIS_HOST` | Redis 호스트 | `redis` |
| `RABBITMQ_HOST` | RabbitMQ 호스트 | `rabbitmq` |
| `JWT_SECRET` | JWT 서명 키 (256bit+) | `your-256-bit-secret-key...` |
| `DEVICE_APP_SECRET` | 디바이스 인증 키 | `device-secret-key` |
