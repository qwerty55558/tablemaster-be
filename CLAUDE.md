# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build              # Build and run tests
./gradlew bootRun            # Run application (port 8080)
./gradlew test               # Run all tests
./gradlew test --tests "TestClassName"  # Run single test class
./gradlew test --tests "TestClassName.methodName"  # Run single test method
./gradlew bootJar            # Build executable JAR
./gradlew clean              # Clean build artifacts
```

## Architecture Overview

Spring Boot 4.0.1 backend with JWT authentication, RabbitMQ messaging, and WebSocket real-time communication.

### Layered Architecture
```
Controller → Service → Repository → Entity (JPA/PostgreSQL)
```

### Key Patterns

**Event-Driven Messaging (RabbitMQ)**
- Producers in `messaging/producer/` publish domain events
- Consumers in `messaging/consumer/` process asynchronously
- Event types: Chat, Table, Gift, Notification
- Dead letter queue for failed messages with retry (3 attempts, exponential backoff)

**Dual Authentication System**
- User tokens: Access (30 min) + Refresh (14 days) with roles (USER, STAFF, ADMIN)
- Device tokens: IoT devices with ROLE_DEVICE
- Token blacklist in Redis for immediate revocation
- Rate limiting in Redis (login: 5/min per IP)

**WebSocket/STOMP** (`/ws` endpoint)
- JWT auth at STOMP frame level via `JwtChannelInterceptor`
- Session registry tracks device/user connections
- Prefixes: `/app` (client→server), `/topic`, `/queue` (server→client)

### Package Structure
- `config/` - Spring configuration and properties classes
- `security/` - JWT provider, filters, and handlers
- `websocket/` - STOMP configuration and session management
- `messaging/producer|consumer/` - RabbitMQ event handlers
- `event/` - Domain event classes and enums
- `dto/` - Request/response DTOs organized by domain

## Required Services

PostgreSQL, Redis, RabbitMQ must be running locally or configured via environment variables:

```bash
JWT_SECRET=<256-bit key>
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
DEVICE_APP_SECRET=<device auth secret>
```

## Database

- PostgreSQL with Hibernate auto-update (`ddl-auto: update`)
- Schema in `src/main/resources/schema.sql` (views and additional DDL)
- Main entities: User, RefreshToken, TableEntity, DeviceWhitelist, Notification

## API Documentation

Swagger UI available at `/swagger-ui.html` when running (SpringDoc OpenAPI).
