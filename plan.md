# JWT 인증 시스템 구현 명세

## 개요

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 4.0.1 |
| Java 버전 | 25 |
| JWT 라이브러리 | JJWT 0.13.0 |
| 세션 저장소 | Redis |
| 아키텍처 | BFF (Backend For Frontend) |

### 아키텍처 구조

```
브라우저 ←────────→ Next.js (BFF) ←────────→ Spring Boot (REST API)
          세션/쿠키        서버간 HTTP (JSON Body)

Flutter App ←─────────────────────────────→ Spring Boot (REST API)
                     HTTP (JSON Body)
```

- Spring Boot는 **순수 REST API 서버**로 동작
- 웹/앱 구분 없이 **동일한 JSON 응답** 제공
- 쿠키 방식 제거 (BFF 아키텍처에서 불필요)
- Next.js(BFF)가 브라우저 세션/쿠키 관리 담당

---

## 토큰 설정

| 항목 | 값 | 비고 |
|------|------|------|
| Access Token 만료 | 30분 | 1800000ms |
| Refresh Token 만료 | 14일 | 1209600000ms |
| 알고리즘 | HS256 | HMAC-SHA256 |
| Access Token 저장 | 클라이언트 메모리 | 요청 시 Authorization 헤더 |
| Refresh Token 저장 | DB (PostgreSQL) | 갱신/무효화 관리 |

---

## Role 구조

```java
public enum Role {
    ROLE_USER,   // 일반 사용자
    ROLE_STAFF,  // 스태프 (회원가입 기본값)
    ROLE_ADMIN   // 관리자
}
```

- 다중 역할 지원: `@ElementCollection`
- URL별 접근 제어:
  - `/api/v1/admin/**` → ROLE_ADMIN만
  - `/api/v1/staff/**` → ROLE_STAFF, ROLE_ADMIN

---

## API 엔드포인트

| 엔드포인트 | 메서드 | 인증 | 설명 |
|------------|--------|------|------|
| `/api/v1/auth/signup` | POST | X | 회원가입 |
| `/api/v1/auth/check-email` | POST | X | 이메일 중복 확인 |
| `/api/v1/auth/login` | POST | X | 로그인, 토큰 발급 |
| `/api/v1/auth/refresh` | POST | X | Access Token 갱신 |
| `/api/v1/auth/logout` | POST | X | 로그아웃 (토큰 무효화) |
| `/api/v1/auth/logout-all` | POST | O | 전체 로그아웃 (모든 기기) |
| `/api/v1/auth/change-password` | POST | O | 비밀번호 변경 + 새 토큰 발급 |
| `/api/v1/config/auth` | GET | X | 인증 설정 정보 조회 |

---

## API 상세 명세

### 1. 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "agreeService": true,
  "agreePrivacy": true,
  "agreeMarketing": false
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "user@example.com",
  "phone": "01012345678",
  "createdAt": "2024-01-08T12:00:00"
}
```

### 2. 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

### 3. 토큰 갱신

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800
}
```

### 4. 로그아웃

```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200 OK)**
```json
{
  "success": true
}
```

### 5. 전체 로그아웃

```http
POST /api/v1/auth/logout-all
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true
}
```

### 6. 비밀번호 변경

```http
POST /api/v1/auth/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "logoutOtherDevices": true
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

---

## Access Token 클레임 구조

```json
{
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "sub": "1",
  "email": "user@example.com",
  "name": "홍길동",
  "roles": ["ROLE_STAFF"],
  "type": "access",
  "iat": 1704672000,
  "exp": 1704673800
}
```

| 클레임 | 설명 |
|--------|------|
| `jti` | JWT ID (블랙리스트용 UUID) |
| `sub` | 사용자 ID |
| `email` | 이메일 |
| `name` | 이름 |
| `roles` | 역할 목록 |
| `type` | 토큰 타입 (access/refresh) |
| `iat` | 발급 시간 |
| `exp` | 만료 시간 |

---

## 보안 기능

### 1. Rate Limiting (Redis)

| 항목 | 제한 | 윈도우 | Redis Key |
|------|------|--------|-----------|
| 로그인 시도 | IP당 5회 | 1분 | `rate:login:{ip}` |
| 토큰 갱신 | 토큰당 10회 | 1분 | `rate:refresh:{tokenHash}` |

### 2. Access Token 블랙리스트 (Redis)

JWT는 stateless 특성상 즉시 무효화 불가 → Redis 블랙리스트로 해결

| 시나리오 | Access Token | Refresh Token |
|----------|--------------|---------------|
| 로그아웃 | 블랙리스트 등록 | DB에서 무효화 |
| 전체 로그아웃 | 블랙리스트 등록 | 모든 토큰 무효화 |
| 비밀번호 변경 | 블랙리스트 등록 | 모든 토큰 무효화 |

**Redis Key 형식**
```
token:blacklist:{jti}
├── value: 등록시간(ms)
└── TTL: 토큰 남은 만료시간 (자동 삭제)
```

### 3. 비밀번호 정책

| 항목 | 값 |
|------|------|
| 최소 길이 | 8자 |
| 최대 길이 | 20자 |
| 대문자 필수 | O |
| 소문자 필수 | O |
| 숫자 필수 | O |
| 특수문자 필수 | O |

---

## 패키지 구조

```
com.mycompany.tablemaster
├── config
│   ├── properties
│   │   ├── AuthValidationProperties.java   # 유효성 검사 설정
│   │   └── JwtProperties.java              # JWT 설정
│   └── SecurityConfig.java                 # Spring Security 설정
│
├── security
│   ├── JwtTokenProvider.java               # 토큰 생성/검증
│   ├── JwtAuthenticationFilter.java        # 요청 필터 (블랙리스트 체크)
│   ├── JwtAuthenticationEntryPoint.java    # 401 처리
│   └── JwtAccessDeniedHandler.java         # 403 처리
│
├── controller
│   ├── AuthController.java                 # 인증 API
│   ├── ConfigController.java               # 설정 API
│   └── HealthController.java               # 헬스체크
│
├── service
│   ├── AuthService.java                    # 인증 비즈니스 로직
│   ├── RateLimitService.java               # Rate Limiting
│   └── TokenBlacklistService.java          # 토큰 블랙리스트
│
├── repository
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── TermsRepository.java
│   └── UserTermsAgreementRepository.java
│
├── entity
│   ├── User.java                           # 사용자 (다중 역할)
│   ├── Role.java                           # 역할 enum
│   ├── RefreshToken.java                   # Refresh Token
│   ├── Terms.java                          # 약관
│   └── UserTermsAgreement.java             # 약관 동의 이력
│
├── dto
│   ├── auth
│   │   ├── SignUpRequest.java
│   │   ├── SignUpResponse.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── TokenRefreshRequest.java
│   │   ├── TokenRefreshResponse.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── EmailCheckRequest.java
│   │   └── EmailCheckResponse.java
│   └── config
│       └── AuthConfigResponse.java
│
└── exception
    └── BusinessException.java              # 비즈니스 예외
```

---

## 환경 변수

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}                    # 최소 256bit (32자 이상)
  access-token-expiration: 1800000         # 30분
  refresh-token-expiration: 1209600000     # 14일

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

auth:
  validation:
    email:
      max-length: 100
    password:
      min-length: 8
      max-length: 20
      require-uppercase: true
      require-lowercase: true
      require-number: true
      require-special-char: true
    name:
      min-length: 2
      max-length: 20
    phone:
      pattern: "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$"
```

---

## 에러 코드

| 코드 | HTTP Status | 메시지 |
|------|-------------|--------|
| AUTH_001 | 409 | 이미 사용 중인 이메일입니다 |
| AUTH_002 | 400 | 필수 약관에 동의해주세요 |
| AUTH_003 | 404 | 약관 정보를 찾을 수 없습니다 |
| AUTH_004 | 400 | 이메일은 N자 이하로 입력해주세요 |
| AUTH_005 | 400 | 비밀번호는 N자 이상 M자 이하로 입력해주세요 |
| AUTH_006 | 400 | 이름은 N자 이상 M자 이하로 입력해주세요 |
| AUTH_007 | 400 | 비밀번호에 대문자를 포함해주세요 |
| AUTH_008 | 400 | 비밀번호에 소문자를 포함해주세요 |
| AUTH_009 | 400 | 비밀번호에 숫자를 포함해주세요 |
| AUTH_010 | 400 | 비밀번호에 특수문자를 포함해주세요 |
| AUTH_011 | 404 | 존재하지 않는 사용자입니다 |
| AUTH_012 | 401 | 비밀번호가 일치하지 않습니다 |
| AUTH_013 | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| AUTH_014 | 401 | 유효하지 않은 Refresh Token입니다 |
| AUTH_015 | 401 | 만료된 Refresh Token입니다 |
| AUTH_016 | 401 | 폐기된 Refresh Token입니다 |
| AUTH_017 | 429 | 로그인 시도 횟수를 초과했습니다 |
| AUTH_018 | 429 | 토큰 갱신 요청이 너무 많습니다 |
| AUTH_019 | 400 | 현재 비밀번호가 일치하지 않습니다 |

---

## 구현 완료 현황

### Phase 1: 기본 JWT 인증
- [x] JJWT 0.13.0 의존성 추가
- [x] JwtProperties 설정 클래스
- [x] JwtTokenProvider (토큰 생성/검증)
- [x] JwtAuthenticationFilter (요청 필터)
- [x] JwtAuthenticationEntryPoint (401 처리)
- [x] JwtAccessDeniedHandler (403 처리)
- [x] SecurityConfig (Spring Security 설정)
- [x] Role enum + User 다중 역할 지원
- [x] RefreshToken 엔티티 + Repository
- [x] AuthService (login, refresh, logout)
- [x] AuthController

### Phase 2: 보안 강화
- [x] RateLimitService (Redis 기반)
- [x] 로그인 시도 제한 (IP당 5회/분)
- [x] 토큰 갱신 제한 (토큰당 10회/분)
- [x] 로그인 성공 시 카운터 리셋

### Phase 3: Access Token 블랙리스트
- [x] JwtTokenProvider에 jti(JWT ID) 클레임 추가
- [x] TokenBlacklistService (Redis 기반)
- [x] JwtAuthenticationFilter 블랙리스트 체크
- [x] logout/logoutAll 블랙리스트 등록
- [x] 비밀번호 변경 API (기존 토큰 무효화)

### Phase 4: BFF 아키텍처 정리
- [x] X-Client-Type 헤더 분기 제거
- [x] 쿠키 방식 제거 (CookieUtil 삭제)
- [x] 웹/앱 통일된 Body 기반 토큰 전달
- [x] LoginResponse 단순화

---

## 클라이언트 연동 가이드

### Next.js (Auth.js v5)

```typescript
// auth.ts
import NextAuth from "next-auth"
import Credentials from "next-auth/providers/credentials"

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [
    Credentials({
      credentials: {
        email: {},
        password: {},
      },
      authorize: async (credentials) => {
        const res = await fetch(`${process.env.API_URL}/api/v1/auth/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(credentials),
        })
        
        if (!res.ok) return null
        
        const data = await res.json()
        return {
          id: String(data.sub),
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          expiresAt: Date.now() + data.expiresIn * 1000,
        }
      },
    }),
  ],
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.accessToken = user.accessToken
        token.refreshToken = user.refreshToken
        token.expiresAt = user.expiresAt
      }
      
      // Access Token 만료 시 갱신
      if (Date.now() > (token.expiresAt as number)) {
        return await refreshAccessToken(token)
      }
      
      return token
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string
      // refreshToken은 session에 포함하지 않음 (클라이언트 노출 방지)
      return session
    },
  },
})

async function refreshAccessToken(token: any) {
  try {
    const res = await fetch(`${process.env.API_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: token.refreshToken }),
    })
    
    if (!res.ok) {
      return { ...token, error: "RefreshTokenError" }
    }
    
    const data = await res.json()
    return {
      ...token,
      accessToken: data.accessToken,
      expiresAt: Date.now() + data.expiresIn * 1000,
    }
  } catch {
    return { ...token, error: "RefreshTokenError" }
  }
}
```

### Flutter

```dart
class AuthApi {
  final Dio dio;
  
  AuthApi(this.dio);
  
  Future<LoginResponse> login(String email, String password) async {
    final response = await dio.post(
      '/api/v1/auth/login',
      data: {'email': email, 'password': password},
    );
    
    final data = response.data;
    await SecureStorage.saveTokens(
      data['accessToken'], 
      data['refreshToken'],
    );
    return LoginResponse.fromJson(data);
  }
  
  Future<String> refreshToken() async {
    final refreshToken = await SecureStorage.getRefreshToken();
    final response = await dio.post(
      '/api/v1/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    
    final newAccessToken = response.data['accessToken'];
    await SecureStorage.saveAccessToken(newAccessToken);
    return newAccessToken;
  }
  
  Future<void> logout() async {
    final accessToken = await SecureStorage.getAccessToken();
    final refreshToken = await SecureStorage.getRefreshToken();
    
    await dio.post(
      '/api/v1/auth/logout',
      data: {'refreshToken': refreshToken},
      options: Options(headers: {
        'Authorization': 'Bearer $accessToken',
      }),
    );
    
    await SecureStorage.clearTokens();
  }
}
```

---

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

---

## 참고

- JJWT GitHub: https://github.com/jwtk/jjwt
- Spring Security: https://spring.io/projects/spring-security
- Auth.js (NextAuth v5): https://authjs.dev
