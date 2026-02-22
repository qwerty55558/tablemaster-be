# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build              # Build and run tests
./gradlew bootRun            # Run application (port 8080)
./gradlew test               # Run all tests
./gradlew test --tests "TestClassName"
./gradlew test --tests "TestClassName.methodName"
./gradlew bootJar            # Build executable JAR
./gradlew clean
```

Stack: Spring Boot 4.0.1, Java 25, Kotlin DSL (`build.gradle.kts`).

## Local Docker Services

Redis must be started with port binding (6379 is not exposed by default):

```bash
docker run -d --name redis-cache -p 6379:6379 redis:latest
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
docker run -d --name postgres-db -p 5432:5432 postgres:latest
```

Environment variables (`.env` file, loaded via `spring.config.import`):

```
JWT_SECRET=<256-bit key>
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
DEVICE_APP_SECRET=<device auth secret>
```

## Architecture Overview

```
Controller → Service → Repository → Entity (JPA/PostgreSQL)
```

Real-time flow: `Service → WebSocketSenderService → STOMP` and `Service → RabbitMQ Producer → Consumer → WebSocketSenderService`

## Key Domain Concepts

### TableEntity (`entity/TableEntity.java`)
- **PK is `deviceId` (String)** — not a numeric ID. The table row is owned by the device.
- `name` = the human-readable table label set by the device app (e.g. "A1").
- Status lifecycle: `AVAILABLE → OCCUPIED → CHATTING → INACTIVE`
- `INACTIVE` = device disconnected; `previousStatus` saves the pre-disconnect state for restore on reconnect.
- `getAllTables()` excludes `AVAILABLE` and `INACTIVE` statuses (only returns tables in active use).

### TableStatus
```
AVAILABLE  – no guests
OCCUPIED   – guests seated
CHATTING   – chat request in progress
INACTIVE   – device WebSocket disconnected (previousStatus saved for restore)
```

### Device Registration Flow
1. Device calls `POST /api/v1/device/register/request` → stored in Redis under `device:pending:{deviceId}` with 3-minute TTL.
2. Admin receives WebSocket notification (`DEVICE_REGISTRATION_REQUEST` on `/topic/role.ADMIN`).
3. Admin calls `POST /api/v1/admin/devices/approve/{deviceId}` → moved from Redis to `device_whitelist` DB table.
4. If TTL expires, Redis key expiration fires `RedisKeyExpirationListener` → notifies admin (`DEVICE_REGISTRATION_EXPIRED`).

## Authentication

**Dual token system — both share one `JwtTokenProvider`:**
- User tokens: claims include `email`, `name`, `roles` (USER/STAFF/ADMIN), `type: access`
- Device tokens: claims include `deviceId`, `deviceName`, `roles: [ROLE_DEVICE]`, `type: access`
- `getAuthentication()` checks for `deviceId` claim to decide `DeviceAuthPrincipal` vs `UserAuthPrincipal`

**Redis usage:**
- `TokenBlacklistService` — blacklists JTI on logout, keyed by JTI with remaining TTL
- `RateLimitService` — login rate limiting (5/min per IP)
- `device:pending:{deviceId}` — pending device registration (TTL 180s)

**Security route rules** (`SecurityConfig`):
- `/api/v1/auth/**`, `/api/v1/config/**`, `/swagger-ui/**`, `/ws/**` — public
- `GET /api/v1/admin/devices` and `GET /api/v1/admin/devices/*` — STAFF or ADMIN
- `/api/v1/admin/**` — ADMIN only
- `/api/v1/staff/**` — STAFF or ADMIN
- `/api/v1/device/**` — DEVICE role only
- `@PreAuthorize` is enabled for method-level checks

## WebSocket / STOMP

**Endpoint:** `/ws` (auth handled at STOMP frame level by `JwtChannelInterceptor`, not HTTP)

**Session attributes** set during CONNECT (used for connect/disconnect events):
```
type      → "DEVICE" or "USER"
id        → deviceId or userId
jti       → token JTI (for blacklist check)
expiresAt → token expiry as Instant
```

**On device CONNECT:** `tableService.activateTable(deviceId)` restores previous status; undelivered notifications are sent.
**On device DISCONNECT:** `tableService.deactivateTable(deviceId)` sets status to `INACTIVE`.

**Destinations:**
- `/queue/device` — device control messages (DEVICE_DELETED, etc.)
- `/queue/tables` — table list updates for a specific device
- `/queue/myTable` — this device's own table updates
- `/queue/chat` — chat messages
- `/queue/notifications` — push/toast notifications
- `/topic/tables` — broadcast table changes to all subscribers
- `/topic/role.ADMIN` / `/topic/role.STAFF` — role-targeted broadcast

`WebSocketSessionRegistry` — in-memory `ConcurrentHashMap` (single-server only; not Redis-backed).

## RabbitMQ

All queues have DLX configured → `dead.letter.queue` on failure.

| Exchange | Type | Queues |
|---|---|---|
| `gift.exchange` | Direct | `gift.process.queue`, `gift.complete.queue` |
| `chat.exchange` | Topic | `chat.message.queue` (routing: `chat.message.#`) |
| `notification.exchange` | Fanout | `notification.push.queue`, `notification.inapp.queue` |
| `table.exchange` | Direct | `table.deleted.queue` |

Constants defined in `RabbitMQConfig` — always use those constants, never hardcode queue/exchange names.

## Error Handling

`BusinessException` is the single runtime exception type. Use static factory methods for domain errors:
```java
throw BusinessException.deviceNotFound();   // DEVICE_002
throw BusinessException.invalidAppSecret(); // DEVICE_001
throw BusinessException.userNotFound();     // AUTH_011
```
Error codes follow `DOMAIN_NNN` format. Add new static factories to `BusinessException` for new domain errors.

## API Documentation

Swagger UI: `/swagger-ui.html` when running. MCP swagger reconnects on app restart.

## Build 
* 디벨롭 모드로 개발 중이니 빌드는 따로 하지 말 것 명령 이후 변경사항 요약만
* 임의로 커밋, 푸시 하지말 것
