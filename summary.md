# 📋 Tablemaster Backend - 프로젝트 구조 분석

> 🗓️ 분석일: 2026-01-21
> 📦 프로젝트: Spring Boot 4.0.1 + Java 25
> 🎯 목적: 테이블 관리 시스템 (IoT 디바이스 + 실시간 통신)

---

## 📌 Overview

```
┌──────────────────────────────────────────────────────────────────┐
│ 🏷️ 프로젝트명    │ tablemaster                                  │
├──────────────────┼───────────────────────────────────────────────┤
│ 🚀 Spring Boot   │ 4.0.1                                         │
│ ☕ Java          │ 25                                            │
│ 📦 패키지        │ com.mycompany.tablemaster                     │
│ 🛠️ 빌드 도구     │ Gradle (Kotlin DSL)                           │
│ 🗄️ 데이터베이스  │ PostgreSQL + Redis                            │
│ 📬 메시징        │ RabbitMQ                                      │
│ 🔌 실시간 통신   │ WebSocket (STOMP)                             │
└──────────────────┴───────────────────────────────────────────────┘
```

---

## 🗂️ 디렉토리 구조

### 루트 디렉토리

```
be/
├── 📁 src/                  # 소스 코드
│   ├── main/java/           # Java 소스
│   └── main/resources/      # 설정 파일
├── 📁 gradle/               # Gradle Wrapper
├── 📁 docs/                 # 문서
├── 📄 build.gradle.kts      # Gradle 빌드 스크립트
├── 📄 settings.gradle.kts   # Gradle 설정
├── 📄 CLAUDE.md             # Claude Code 지침
└── 📄 .env.example          # 환경변수 예시
```

### 소스 코드 구조 (src/main/java)

```
com.mycompany.tablemaster/
│
├── 📄 TablemasterApplication.java    # 🚀 메인 진입점
│
├── 📁 config/                        # ⚙️ 설정
│   ├── 📁 properties/                # 속성 클래스
│   ├── SecurityConfig.java
│   ├── RabbitMQConfig.java
│   ├── WebSocketConfig.java
│   └── OpenApiConfig.java
│
├── 📁 controller/                    # 🎮 REST API
│   ├── AuthController.java
│   ├── DeviceAuthController.java
│   ├── DeviceAdminController.java
│   ├── TableController.java
│   ├── NotificationController.java
│   ├── SyncController.java
│   ├── ConfigController.java
│   └── HealthController.java
│
├── 📁 service/                       # 💼 비즈니스 로직
│   ├── AuthService.java
│   ├── DeviceAuthService.java
│   ├── TableService.java
│   ├── NotificationService.java
│   ├── SyncService.java
│   ├── TokenBlacklistService.java
│   ├── RateLimitService.java
│   └── WebSocketSenderService.java
│
├── 📁 repository/                    # 🗄️ 데이터 접근
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── TableRepository.java
│   ├── DeviceWhitelistRepository.java
│   ├── NotificationRepository.java
│   └── ... (기타 레포지토리)
│
├── 📁 entity/                        # 📊 JPA 엔티티
│   ├── User.java
│   ├── TableEntity.java
│   ├── DeviceWhitelist.java
│   ├── Notification.java
│   ├── RefreshToken.java
│   └── ... (기타 엔티티)
│
├── 📁 dto/                           # 📦 데이터 전송 객체
│   ├── 📁 auth/
│   ├── 📁 device/
│   ├── 📁 table/
│   ├── 📁 notification/
│   ├── 📁 sync/
│   └── 📁 config/
│
├── 📁 security/                      # 🔐 보안
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── ... (기타 보안 클래스)
│
├── 📁 websocket/                     # 🔌 WebSocket
│   ├── JwtChannelInterceptor.java
│   ├── WebSocketSessionRegistry.java
│   ├── WebSocketEventListener.java
│   └── ... (기타 WebSocket 클래스)
│
├── 📁 messaging/                     # 📬 RabbitMQ
│   ├── 📁 producer/
│   └── 📁 consumer/
│
├── 📁 event/                         # 📢 도메인 이벤트
│   ├── TableEvent.java
│   ├── ChatEvent.java
│   ├── GiftEvent.java
│   └── NotificationEvent.java
│
└── 📁 exception/                     # ⚠️ 예외 처리
    └── BusinessException.java
```

---

## 🏗️ 아키텍처

### 전체 시스템 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           📱 클라이언트                                      │
│                    (Flutter App / Next.js Web)                              │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
        ┌───────────────────────┴───────────────────────┐
        │                                               │
        ▼                                               ▼
┌───────────────┐                              ┌───────────────┐
│  🌐 REST API  │                              │ 🔌 WebSocket  │
│  (HTTP + JWT) │                              │ (STOMP + JWT) │
└───────┬───────┘                              └───────┬───────┘
        │                                               │
        └───────────────────────┬───────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        🔐 Spring Security                                  │
│              JwtAuthenticationFilter / JwtChannelInterceptor              │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         🎮 Controller Layer                                │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                          💼 Service Layer                                  │
└───────────┬───────────────────┬───────────────────────┬───────────────────┘
            │                   │                       │
            ▼                   ▼                       ▼
┌───────────────────┐  ┌───────────────────┐  ┌───────────────────────────┐
│  🗄️ Repository    │  │   🔴 Redis        │  │     🐰 RabbitMQ          │
│  (JPA/PostgreSQL) │  │  (Cache/Limit)    │  │  (Event Messaging)       │
└───────────────────┘  └───────────────────┘  └───────────────────────────┘
```

---

## 📁 패키지별 상세 설명

### ⚙️ config/ - 설정 클래스

```
┌─────────────────────────────┬────────────────────────────────────────────────┐
│ 클래스                       │ 기능                                           │
├─────────────────────────────┼────────────────────────────────────────────────┤
│ SecurityConfig              │ Spring Security 설정 (JWT, CORS, URL 권한)     │
│ RabbitMQConfig              │ Exchange/Queue/Binding, Dead Letter Queue      │
│ WebSocketConfig             │ STOMP WebSocket 설정, JWT 인터셉터             │
│ OpenApiConfig               │ Swagger/OpenAPI 문서화                         │
└─────────────────────────────┴────────────────────────────────────────────────┘
```

**properties/ 하위:**

```
┌─────────────────────────────┬────────────────────────────────────────────────┐
│ 클래스                       │ 기능                                           │
├─────────────────────────────┼────────────────────────────────────────────────┤
│ JwtProperties               │ JWT secret, 토큰 만료시간                       │
│ DeviceProperties            │ 디바이스 인증용 App Secret                      │
│ AuthValidationProperties    │ 회원가입 유효성 검사 규칙                       │
└─────────────────────────────┴────────────────────────────────────────────────┘
```

---

### 🎮 controller/ - REST API 컨트롤러

```
┌────────────────────────┬──────────────────────────┬───────────────────────────────┐
│ 컨트롤러                │ 경로                      │ 기능                           │
├────────────────────────┼──────────────────────────┼───────────────────────────────┤
│ AuthController         │ /api/v1/auth/**          │ 회원가입, 로그인, 토큰 갱신    │
│ DeviceAuthController   │ /api/v1/auth/device/**   │ 디바이스 인증                  │
│ DeviceAdminController  │ /api/v1/admin/devices/** │ 디바이스 관리 (관리자)         │
│ TableController        │ /api/v1/tables/**        │ 테이블 CRUD                    │
│ NotificationController │ /api/v1/device/notif/**  │ 알림 조회/읽음 처리            │
│ SyncController         │ WebSocket /app/sync      │ 디바이스 동기화                │
│ ConfigController       │ /api/v1/config/**        │ 클라이언트 설정 조회           │
│ HealthController       │ /api/health              │ 헬스 체크                      │
└────────────────────────┴──────────────────────────┴───────────────────────────────┘
```

---

### 💼 service/ - 비즈니스 로직

```
┌─────────────────────────┬─────────────────────────────────────────────────────────┐
│ 서비스                   │ 기능                                                     │
├─────────────────────────┼─────────────────────────────────────────────────────────┤
│ AuthService             │ 회원가입, 로그인, 토큰 갱신, 로그아웃, 비밀번호 변경     │
│ DeviceAuthService       │ 디바이스 로그인, 등록/승인 (Redis 대기열)                │
│ TableService            │ 테이블 CRUD, 브로드캐스트, 활성화/비활성화               │
│ NotificationService     │ 알림 생성/전송, 읽음 처리                                │
│ SyncService             │ 디바이스 동기화 데이터 조합                              │
│ TokenBlacklistService   │ Redis 기반 Access Token 블랙리스트                       │
│ RateLimitService        │ Redis 기반 Rate Limiting                                 │
│ WebSocketSenderService  │ WebSocket 메시지 전송 (디바이스/사용자/역할별)           │
└─────────────────────────┴─────────────────────────────────────────────────────────┘
```

---

### 📊 entity/ - JPA 엔티티

```
┌─────────────────────────┬─────────────────────────┬────────────────────────────────┐
│ 엔티티                   │ 테이블명                 │ 역할                            │
├─────────────────────────┼─────────────────────────┼────────────────────────────────┤
│ User                    │ users                   │ 사용자 정보 (이메일, 역할)      │
│ RefreshToken            │ refresh_tokens          │ Refresh Token 저장             │
│ TableEntity             │ tables                  │ 테이블 정보 (id=deviceId)      │
│ TableHistory            │ table_history           │ 삭제된 테이블 히스토리          │
│ DeviceWhitelist         │ device_whitelist        │ 허용 디바이스 목록             │
│ Notification            │ notifications           │ 알림 저장                       │
│ Terms                   │ terms                   │ 약관 정보                       │
│ UserTermsAgreement      │ user_terms_agreements   │ 사용자 약관 동의               │
└─────────────────────────┴─────────────────────────┴────────────────────────────────┘
```

**Enum 타입:**

```
┌────────────────────────┬───────────────────────────────────────────────────────────┐
│ Enum                   │ 값                                                         │
├────────────────────────┼───────────────────────────────────────────────────────────┤
│ Role                   │ ROLE_USER, ROLE_STAFF, ROLE_ADMIN                         │
│ TableStatus            │ AVAILABLE, OCCUPIED, RESERVED, CHATTING, INACTIVE         │
│ NotificationCategory   │ ORDER, CHAT, GIFT, SYSTEM, PROMOTION                      │
└────────────────────────┴───────────────────────────────────────────────────────────┘
```

---

### 🔐 security/ - JWT 보안

```
┌──────────────────────────────┬─────────────────────────────────────────────────┐
│ 클래스                        │ 기능                                             │
├──────────────────────────────┼─────────────────────────────────────────────────┤
│ JwtTokenProvider             │ JWT 생성/검증, Claims 추출                       │
│ JwtAuthenticationFilter      │ HTTP 요청 JWT 검증 필터                          │
│ JwtAuthenticationEntryPoint  │ 401 Unauthorized 처리                           │
│ JwtAccessDeniedHandler       │ 403 Forbidden 처리                              │
│ UserAuthPrincipal            │ REST API용 사용자 Principal                      │
│ DeviceAuthPrincipal          │ REST API용 디바이스 Principal                    │
└──────────────────────────────┴─────────────────────────────────────────────────┘
```

---

### 🔌 websocket/ - WebSocket 관련

```
┌────────────────────────────────────┬─────────────────────────────────────────────┐
│ 클래스                              │ 기능                                         │
├────────────────────────────────────┼─────────────────────────────────────────────┤
│ WebSocketConfig                    │ STOMP 설정 (/ws 엔드포인트)                  │
│ JwtChannelInterceptor              │ STOMP CONNECT 시 JWT 검증                    │
│ WebSocketSessionRegistry           │ Device/User 세션 관리                        │
│ WebSocketEventListener             │ 연결/해제 이벤트, 테이블 활성화/비활성화     │
│ WebSocketSessionHandlerDecorator   │ 세션 등록/해제 데코레이터                    │
│ WebSocketTokenValidator            │ 1분 주기 토큰 만료/블랙리스트 검증           │
│ WebSocketPrincipal (interface)     │ Device/User 공통 Principal 인터페이스        │
│ DevicePrincipal                    │ Device용 WebSocket Principal                 │
│ UserPrincipal                      │ User용 WebSocket Principal                   │
└────────────────────────────────────┴─────────────────────────────────────────────┘
```

---

### 📬 messaging/ - RabbitMQ

**Producer (메시지 발행):**

```
┌────────────────────────────┬──────────────────────┬────────────────────────────┐
│ 프로듀서                    │ Exchange             │ 기능                        │
├────────────────────────────┼──────────────────────┼────────────────────────────┤
│ TableEventProducer         │ table.exchange       │ 테이블 이벤트              │
│ ChatEventProducer          │ chat.exchange        │ 채팅 메시지/입장/퇴장      │
│ GiftEventProducer          │ gift.exchange        │ 선물 처리/완료             │
│ NotificationEventProducer  │ notification.exchange│ 알림 브로드캐스트          │
└────────────────────────────┴──────────────────────┴────────────────────────────┘
```

**Consumer (메시지 소비):**

```
┌────────────────────────────┬──────────────────────────┬─────────────────────────┐
│ 컨슈머                      │ Queue                    │ 기능                     │
├────────────────────────────┼──────────────────────────┼─────────────────────────┤
│ TableEventConsumer         │ table.deleted.queue      │ 테이블 삭제 알림        │
│ ChatEventConsumer          │ chat.message.queue       │ 채팅 메시지 브로드캐스트│
│ GiftEventConsumer          │ gift.process/complete    │ 선물 처리               │
│ NotificationEventConsumer  │ notification.push/inapp  │ 푸시/인앱 알림          │
└────────────────────────────┴──────────────────────────┴─────────────────────────┘
```

---

### 📦 dto/ - 데이터 전송 객체

```
┌────────────────┬─────────────────────────────────────────────────────────────────┐
│ 패키지          │ 주요 DTO                                                         │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ auth/          │ LoginRequest/Response, SignUpRequest/Response,                  │
│                │ TokenRefreshRequest/Response, ChangePasswordRequest             │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ device/        │ DeviceLoginRequest/Response, DeviceRegisterRequest,             │
│                │ DeviceApproveRequest, DeviceResponse, DevicePendingResponse     │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ table/         │ TableSetupRequest/Response, TableListResponse,                  │
│                │ TableUpdateRequest                                              │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ notification/  │ NotificationDTO                                                 │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ sync/          │ SyncRequest, SyncResponse                                       │
├────────────────┼─────────────────────────────────────────────────────────────────┤
│ config/        │ AuthConfigResponse                                              │
└────────────────┴─────────────────────────────────────────────────────────────────┘
```

---

### 📢 event/ - 도메인 이벤트

```
┌─────────────────────┬────────────────────────────────────────────────────────────┐
│ 이벤트               │ 타입                                                        │
├─────────────────────┼────────────────────────────────────────────────────────────┤
│ TableEvent          │ SETUP, UPDATED, DELETED                                    │
│ ChatEvent           │ MESSAGE, JOIN, LEAVE                                       │
│ GiftEvent           │ CREATED, PROCESSING, COMPLETED, FAILED                     │
│ NotificationEvent   │ PUSH, IN_APP                                               │
└─────────────────────┴────────────────────────────────────────────────────────────┘
```

---

## ⚙️ 설정 파일

### application.yaml (공통)

```yaml
📍 src/main/resources/application.yaml

• 애플리케이션명: tablemaster
• 서버 포트: 8080
• JPA Dialect: PostgreSQL
• RabbitMQ: manual ack, retry 3회
• JWT 만료시간: Access 30분, Refresh 14일
• 인증 유효성 검사 규칙 (이메일, 비밀번호, 이름, 전화번호)
```

### application-dev.yaml (개발)

```yaml
📍 src/main/resources/application-dev.yaml

• PostgreSQL: localhost:5432
• Redis: localhost:6379
• RabbitMQ: localhost:5672 (guest/guest)
• JPA: ddl-auto=update, show-sql=true
• 로깅: DEBUG 레벨
```

### application-prod.yaml (운영)

```yaml
📍 src/main/resources/application-prod.yaml

• 모든 설정 환경변수 기반
• JPA: ddl-auto=validate, show-sql=false
• HikariCP: pool 20, timeout 설정
• 로깅: WARN/INFO 레벨
```

### build.gradle.kts

```
📍 루트/build.gradle.kts

📦 주요 의존성:
├── spring-boot-starter-webmvc, security, data-jpa, websocket, validation
├── spring-boot-starter-data-redis, amqp (RabbitMQ)
├── springdoc-openapi-starter-webmvc-ui (Swagger)
├── jjwt 0.13.0 (JWT)
├── postgresql (드라이버)
├── lombok
└── micrometer-prometheus (모니터링)
```

---

## 🔑 주요 기능

### 1. 🔐 인증 시스템

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           이중 인증 시스템                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  👤 User 토큰                          📱 Device 토큰                        │
│  ├── Access Token (30분)               ├── ROLE_DEVICE                      │
│  ├── Refresh Token (14일)              └── deviceId claim                   │
│  └── Role: USER / STAFF / ADMIN                                             │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│  🔴 Redis 기반 보안                                                          │
│  ├── Token Blacklist: 로그아웃 시 즉시 무효화                                │
│  └── Rate Limiting: 로그인 5회/분, 토큰 갱신 10회/분                         │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2. 🪑 테이블 관리

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           테이블 상태 관리                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  📌 테이블 ID = Device ID (PK)                                               │
│                                                                              │
│  상태 흐름:                                                                   │
│  ┌───────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ AVAILABLE │ ──▶│ OCCUPIED │ ──▶│ CHATTING │ ──▶│ INACTIVE │              │
│  └───────────┘    └──────────┘    └──────────┘    └──────────┘              │
│                                                                              │
│  ✨ 디바이스 연결 해제 시 자동 INACTIVE                                       │
│  ✨ 재연결 시 이전 상태 복원                                                  │
│  ✨ WebSocket 브로드캐스트로 실시간 동기화                                    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3. 🔌 WebSocket 통신

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          WebSocket (STOMP)                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  🌐 엔드포인트: /ws (SockJS 지원)                                            │
│  🔐 STOMP 프레임 레벨 JWT 인증                                               │
│  📊 Device/User 세션 분리 관리                                               │
│                                                                              │
│  📬 채널 (Queue - 개인):                                                     │
│  ├── /queue/tables          → 전체 테이블 목록                               │
│  ├── /queue/myTable         → 내 테이블 정보                                 │
│  ├── /queue/notifications   → 알림                                           │
│  └── /queue/chat            → 채팅 메시지                                    │
│                                                                              │
│  📢 토픽 (Topic - 그룹):                                                     │
│  ├── /topic/role.ADMIN      → 관리자 전용                                    │
│  └── /topic/chat.room.{id}  → 채팅방                                         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4. 📬 메시징 (RabbitMQ)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          RabbitMQ 이벤트 기반 아키텍처                        │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Exchange 타입:                                                              │
│  ├── gift.exchange        (Direct)  → 선물 처리                              │
│  ├── chat.exchange        (Topic)   → 채팅 메시지                            │
│  ├── notification.exchange(Fanout)  → 알림 브로드캐스트                      │
│  └── table.exchange       (Direct)  → 테이블 이벤트                          │
│                                                                              │
│  ⚠️ Dead Letter Queue:                                                       │
│  └── 실패 메시지 재시도 3회 후 DLQ로 이동                                    │
│                                                                              │
│  ✅ Manual Ack 모드로 메시지 손실 방지                                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 실행 방법

### 필수 서비스

```bash
# PostgreSQL, Redis, RabbitMQ 실행 필요
```

### 환경 변수

```bash
JWT_SECRET=<256-bit key>
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
DEVICE_APP_SECRET=<device auth secret>
```

### 빌드 & 실행

```bash
./gradlew build              # 빌드 및 테스트
./gradlew bootRun            # 애플리케이션 실행 (포트 8080)
./gradlew test               # 테스트 실행
```

### API 문서

```
📄 Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 📝 정리

이 프로젝트는 **테이블 관리 시스템**으로, IoT 디바이스(태블릿)와 관리자 웹/앱 간의 실시간 통신을 지원합니다.

**핵심 특징:**
- ✅ JWT 기반 이중 인증 (User / Device)
- ✅ WebSocket(STOMP)을 통한 실시간 동기화
- ✅ RabbitMQ 이벤트 기반 아키텍처
- ✅ Redis 기반 Rate Limiting & Token Blacklist
- ✅ 디바이스 연결 상태에 따른 자동 테이블 활성화/비활성화