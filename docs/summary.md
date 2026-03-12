# 채팅 모니터 시스템 — 워크플로우 & 아키텍처

> `/staff/chat-monitor` 페이지의 목업 데이터를 실 데이터로 전환하기 위한 백엔드 구현 정리.
> 디바이스 간 채팅 요청/수락/메시지/선물/퇴장 플로우 + 스태프 모니터링 REST/WebSocket API.

---

## 1. 데이터 모델

### 1-1. 신규 엔티티

| 엔티티 | 테이블명 | 핵심 필드 | 역할 |
|---|---|---|---|
| `ChatRoom` | `chat_rooms` | `status`, `startedAt`, `closedAt`, `totalMessageCount`, `giftCount`, `reportCount` | 채팅방 + 반정규화 카운터 |
| `ChatRoomParticipant` | `chat_room_participants` | `chatRoom(FK)`, `deviceId`, `tableName`, `isMuted` | 채팅방 ↔ 테이블 매핑 (FK 미사용, 스냅샷) |
| `ChatMessage` | `chat_messages` | `chatRoom(FK)`, `senderDeviceId`, `senderTableName`, `content(TEXT)`, `type` | 메시지 영속화 |
| `ChatReport` | `chat_reports` | `chatRoom(FK)`, `reporterDeviceId`, `reportedDeviceId`, `reason`, `status`, `reviewedBy` | 신고/경고 |
| `StaffChatReadPosition` | `staff_chat_read_positions` | `userId + chatRoomId (unique)`, `lastReadMessageId` | 스태프별 읽음 위치 → 미읽음 배지 |

### 1-2. 신규 Enum

| Enum | 값 |
|---|---|
| `ChatRoomStatus` | `ACTIVE` · `CLOSED` · `SANCTIONED` |
| `ChatMessageType` | `MESSAGE` · `JOIN` · `LEAVE` · `GIFT` · `SYSTEM` |
| `ChatReportStatus` | `PENDING` · `REVIEWED` · `DISMISSED` |

### 1-3. 기존 수정

| 파일 | 변경 |
|---|---|
| `ChatEvent.java` | `senderDeviceId`, `senderTableName`, `messageType` 필드 추가. 디바이스 기반 팩토리 메서드 추가 |
| `ChatEventType.java` | `GIFT` 추가 |
| `BusinessException.java` | `CHAT_001`~`CHAT_006`, `TABLE_001` 팩토리 메서드 추가 |

---

## 2. 서비스 계층

### ChatRoomService

| 메서드 | 하는 일 |
|---|---|
| `createRoom(deviceId1, name1, deviceId2, name2)` | `ChatRoom` + `Participant` 2명 생성, `TableEntity.startChatting()`, 스태프 모니터 `ROOM_CREATED` 발행 |
| `closeRoom(roomId)` | `CLOSED` 처리, `TableEntity.endChatting()`, 스태프 모니터 `ROOM_CLOSED` 발행 |
| `sanctionRoom(roomId, userId)` | `SANCTIONED` 처리, 시스템 메시지 저장, 양쪽 디바이스에 `CHAT_SANCTIONED` 전송, 테이블 상태 복원 |
| `toggleMute(roomId, deviceId)` | `isMuted` 토글 |
| `isMuted(roomId, deviceId)` | 음소거 여부 조회 (메시지 전송 전 체크용) |
| `getActiveRooms()` | `ACTIVE` 채팅방 목록 (최신순) |
| `getParticipants(roomId)` | 참여자 목록 |

### ChatMessageService

| 메서드 | 하는 일 |
|---|---|
| `saveMessage(chatRoom, event)` | `ChatMessage` 저장 + `chatRoom` 카운터 증가 (`totalMessageCount`, GIFT면 `giftCount`도) |
| `saveSystemMessage(chatRoom, content)` | 시스템 메시지 저장 (제재 시 사용) |
| `getMessages(roomId, pageable)` | 페이지네이션 히스토리 (최신순) |
| `updateReadPosition(roomId, userId, lastMessageId)` | `StaffChatReadPosition` upsert |
| `getUnreadCount(roomId, userId)` | `ChatMessage.id > lastReadMessageId` count |

### ChatReportService

| 메서드 | 하는 일 |
|---|---|
| `createReport(roomId, reporter, reported, reason)` | `ChatReport` 저장 + `reportCount` 증가 + 스태프 `REPORT_CREATED` 발행 |
| `reviewReport(reportId, userId, status)` | 검토 처리 |

---

## 3. API 목록

### 3-1. STOMP 엔드포인트 (디바이스 → 서버)

| 경로 | DTO | 설명 |
|---|---|---|
| `/app/chat/request` | `ChatRequestMessage` `{targetDeviceId}` | 채팅 요청 |
| `/app/chat/accept` | `ChatAcceptMessage` `{requesterDeviceId}` | 채팅 수락 → 방 생성 |
| `/app/chat/reject` | `ChatRejectMessage` `{requesterDeviceId}` | 채팅 거절 |
| `/app/chat/send` | `ChatSendMessage` `{roomId, message}` | 메시지 전송 |
| `/app/chat/gift` | `ChatGiftMessage` `{roomId, giftType}` | 선물 전송 |
| `/app/chat/leave` | `ChatSendMessage` `{roomId}` | 채팅 퇴장 → 방 종료 |

### 3-2. REST 엔드포인트 (스태프 전용)

> `SecurityConfig`의 `/api/v1/staff/**` → `ROLE_STAFF` 또는 `ROLE_ADMIN` 필요

| Method | Path | 설명 | UI @ref |
|---|---|---|---|
| `GET` | `/api/v1/staff/chat/rooms?search=&filter=` | 활성 채팅방 목록 (검색 + 필터 + 미읽음) | @e15~e18, @e12, @e3 |
| `GET` | `/api/v1/staff/chat/rooms/{id}` | 채팅방 상세 (참여 테이블 정보) | 오른쪽 패널 |
| `GET` | `/api/v1/staff/chat/rooms/{id}/messages?page=&size=` | 메시지 히스토리 (페이지네이션) | 가운데 패널 |
| `POST` | `/api/v1/staff/chat/rooms/{id}/read` | 읽음 위치 갱신 | 배지 제거 |
| `POST` | `/api/v1/staff/chat/rooms/{id}/sanction` | 채팅방 제재 | @e4 |
| `POST` | `/api/v1/staff/chat/rooms/{id}/mute` | 참여자 음소거 토글 | @e6, @e8 |
| `POST` | `/api/v1/staff/chat/notify/{deviceId}` | 디바이스에 알림 전송 | @e5, @e7 |

### 3-3. WebSocket 토픽 (서버 → 클라이언트)

| 토픽 | 구독자 | 이벤트 타입 |
|---|---|---|
| `/user/{deviceId}/queue/chat` | 디바이스 앱 | `CHAT_REQUEST`, `CHAT_ACCEPTED`, `CHAT_REJECTED`, `CHAT_SANCTIONED`, `CHAT_MUTED`, `STAFF_NOTIFICATION` |
| `/topic/chat.room.{roomId}` | 채팅 참여 디바이스 | `MESSAGE`, `JOIN`, `LEAVE` (+ `messageType`: `TEXT`/`GIFT`) |
| `/topic/staff.chat.monitor` | 스태프 웹 | `ROOM_CREATED`, `ROOM_CLOSED`, `ROOM_SANCTIONED`, `ROOM_UPDATED`, `REPORT_CREATED` |
| `/topic/staff.chat.room.{roomId}` | 스태프 웹 (방 선택 시) | `TEXT`, `GIFT`, `SYSTEM`, `JOIN`, `LEAVE` |

---

## 4. 워크플로우 시나리오

### 시나리오 1 — 채팅 요청 → 수락 → 방 생성

```
A1(device-001)                     서버                          B1(device-002)
      │                              │                                │
      │─── /app/chat/request ───────▶│                                │
      │    {targetDeviceId:          │                                │
      │     "device-002"}            │                                │
      │                              │                                │
      │                              │  ChatController :36            │
      │                              │  ├ tableRepo.findById(001)     │
      │                              │  ├ tableRepo.findById(002)     │
      │                              │  ├ isChatEnabled 체크           │
      │                              │  ├ isChatting 체크              │
      │                              │  │                              │
      │                              │──┼── /queue/chat ──────────────▶│
      │                              │  │   {type: CHAT_REQUEST,       │
      │                              │  │    fromTableName: "A1"}      │
      │                              │                                │
      │                              │◀──── /app/chat/accept ─────────│
      │                              │      {requesterDeviceId:        │
      │                              │       "device-001"}             │
      │                              │                                │
      │                              │  ChatController :76             │
      │                              │  └ chatRoomService.createRoom() │
      │                              │    ├ ChatRoom(ACTIVE) save      │
      │                              │    ├ Participant×2 save         │
      │                              │    ├ A1.startChatting()         │
      │                              │    ├ B1.startChatting()         │
      │                              │    └ broadcast(staff.chat       │
      │                              │        .monitor, ROOM_CREATED)  │
      │                              │                                │
      │◀── /queue/chat ──────────────│──── /queue/chat ──────────────▶│
      │    {type: CHAT_ACCEPTED,     │    {type: CHAT_ACCEPTED,       │
      │     roomId: 1,               │     roomId: 1,                 │
      │     partnerTableName: "B1"}  │     partnerTableName: "A1"}    │
```

**DB 결과:**

```
chat_rooms:       id=1, status=ACTIVE, msg_count=0
participants:     (device-001, A1), (device-002, B1)
tables:           A1→CHATTING, B1→CHATTING
```

---

### 시나리오 2 — 메시지 전송

```
A1(device-001)                     서버                          B1(device-002)
      │                              │                                │
      │─── /app/chat/send ──────────▶│                                │
      │    {roomId:1,                │                                │
      │     message:"안녕하세요!"}    │                                │
      │                              │                                │
      │                              │  ChatController :158            │
      │                              │  ├ isMuted 체크 → false         │
      │                              │  └ ChatEvent.message(...)       │
      │                              │    → chatEventProducer          │
      │                              │      .sendMessage()             │
      │                              │                                │
      │                              │      ┌─ RabbitMQ ─┐            │
      │                              │      │ chat.exchange│           │
      │                              │      └──────┬──────┘            │
      │                              │             ▼                   │
      │                              │  ChatEventConsumer :32          │
      │                              │  │                              │
      │                              │  │ ① 디바이스 브로드캐스트        │
      │◀── /topic/chat.room.1 ───────│──┼── /topic/chat.room.1 ──────▶│
      │    {type:MESSAGE,            │  │   {senderTableName:"A1",     │
      │     message:"안녕하세요!"}    │  │    message:"안녕하세요!"}     │
      │                              │  │                              │
      │                              │  │ ② DB 저장                    │
      │                              │  │   ChatMessage save (id=1)    │
      │                              │  │   room.msgCount → 1          │
      │                              │  │                              │
      │                              │  │ ③ 스태프 목록 갱신            │
      │                              │  │   → /topic/staff.chat.monitor│
      │                              │  │     {ROOM_UPDATED, count:1}  │
      │                              │  │                              │
      │                              │  │ ④ 스태프 실시간 메시지         │
      │                              │  │   → /topic/staff.chat.room.1 │
      │                              │  │     {TEXT, "안녕하세요!"}      │
```

---

### 시나리오 3 — 선물 전송

```
B1(device-002)                     서버
      │                              │
      │─── /app/chat/gift ──────────▶│
      │    {roomId:1,                │
      │     giftType:"CHAMPAGNE"}    │
      │                              │
      │                              │  ChatController :183
      │                              │  └ ChatEvent.gift(...)
      │                              │    → RabbitMQ → ChatEventConsumer
      │                              │      ├ broadcast(chat.room.1)
      │                              │      ├ ChatMessage(type:GIFT) save
      │                              │      ├ room.msgCount++, giftCount++
      │                              │      ├ staff.chat.monitor → ROOM_UPDATED
      │                              │      │   {giftCount: 1}
      │                              │      └ staff.chat.room.1 → {GIFT, "CHAMPAGNE"}
```

> `giftCount` 증가로 목록의 선물 필터(`?filter=GIFT`)에 노출됨.

---

### 시나리오 4 — 스태프 모니터 페이지 진입

```
스태프 브라우저                            서버
      │                                     │
      │  ① 초기 데이터 (REST)                 │
      │─── GET /staff/chat/rooms ──────────▶│
      │                                     │  StaffChatController :41
      │                                     │  ├ getActiveRooms() → [room1]
      │                                     │  ├ getParticipants(1) → [A1, B1]
      │                                     │  ├ getUnreadCount(1, userId)
      │                                     │  │  └ lastReadId=0, count=2 → unread:2
      │                                     │  └ ChatRoomListResponse.from(...)
      │◀── 200 OK ─────────────────────────│
      │    [{id:1, status:ACTIVE,           │
      │      participants:[A1,B1],          │
      │      msgCount:2, giftCount:1,       │
      │      unreadCount:2}]                │
      │                                     │
      │  ② 실시간 구독 (WebSocket)            │
      │─── SUBSCRIBE /topic/staff ──────────▶│
      │    .chat.monitor                    │
      │─── SUBSCRIBE /topic/staff ──────────▶│
      │    .chat.room.1                     │
      │                                     │
      │  ③ 읽음 처리                          │
      │─── POST /rooms/1/read ─────────────▶│
      │    {lastMessageId: 2}               │  updateReadPosition(1, userId, 2)
      │                                     │  → unreadCount: 2 → 0
```

---

### 시나리오 5 — 스태프 제재

```
스태프 브라우저                            서버                        디바이스들
      │                                     │                            │
      │─── POST /rooms/1/sanction ─────────▶│                            │
      │                                     │                            │
      │                                     │  ChatRoomService :118       │
      │                                     │  ├ room.sanction()          │
      │                                     │  │  └ SANCTIONED, closedAt  │
      │                                     │  │                          │
      │                                     │  ├ saveSystemMessage()      │
      │                                     │  │  └ "관리자에 의해         │
      │                                     │  │    채팅이 제재되었습니다"  │
      │                                     │  │                          │
      │                                     │  ├ for each participant:    │
      │                                     │  │  ├ endChatting()          │
      │                                     │  │  │  └ OCCUPIED, false    │
      │                                     │  │  └ sendChatToDevice ────▶│
      │                                     │  │    {CHAT_SANCTIONED}     │
      │                                     │  │                          │
      │◀── staff.chat.monitor ──────────────│  └ broadcast               │
      │    {ROOM_SANCTIONED, roomId:1}      │    (staff.chat.monitor)     │
      │                                     │                            │
      │◀── 200 OK ─────────────────────────│                            │
```

---

### 시나리오 6 — 음소거 후 메시지 차단

```
스태프                                서버                          A1
      │                                │                             │
      │─── POST /rooms/1/mute ────────▶│                             │
      │    {deviceId:"device-001"}     │                             │
      │                                │  toggleMute → isMuted=true  │
      │◀── {isMuted: true} ───────────│                             │
      │                                │                             │
      │                                │◀── /app/chat/send ──────────│
      │                                │    {roomId:1, message:...}  │
      │                                │                             │
      │                                │  ChatController :165         │
      │                                │  isMuted(1, "device-001")   │
      │                                │  → true                     │
      │                                │                             │
      │                                │── /queue/chat ─────────────▶│
      │                                │   {type: CHAT_MUTED}        │
      │                                │                             │
      │                                │  return (RabbitMQ 안 감)     │
```

---

### 시나리오 7 — 정상 퇴장

```
A1(device-001)                     서버                          B1(device-002)
      │                              │                                │
      │─── /app/chat/leave ─────────▶│                                │
      │    {roomId:1}                │                                │
      │                              │  ChatController :137            │
      │                              │  ├ ChatEvent.leave(...)         │
      │                              │  │  → RabbitMQ → Consumer       │
      │                              │  │    └ broadcast(chat.room.1,  │
      │◀── /topic/chat.room.1 ───────│──┼──── {type:LEAVE}) ─────────▶│
      │                              │  │                              │
      │                              │  └ chatRoomService.closeRoom(1) │
      │                              │    ├ room.close() → CLOSED      │
      │                              │    ├ A1.endChatting() → OCCUPIED│
      │                              │    ├ B1.endChatting() → OCCUPIED│
      │                              │    └ staff.chat.monitor         │
      │                              │      → {ROOM_CLOSED, roomId:1}  │
```

---

## 5. 메시지 흐름 경로 요약

### 디바이스 → 디바이스 (채팅 메시지)

```
ChatController.handleChatSend()
  → ChatEventProducer.sendMessage()
    → RabbitMQ (chat.exchange → chat.message.queue)
      → ChatEventConsumer.handleChatMessage()
        → webSocketSenderService.broadcast("chat.room.{id}")
          → /topic/chat.room.{id}  →  양쪽 디바이스 수신
```

### 디바이스 → 스태프 모니터 (실시간 갱신)

```
ChatEventConsumer.handleChatMessage()
  ├→ chatMessageService.saveMessage()       ← DB 저장
  ├→ broadcast("staff.chat.monitor")        ← 목록 카운터 갱신
  └→ broadcast("staff.chat.room.{id}")      ← 선택한 방 실시간 메시지
```

### 디바이스 → 디바이스 (채팅 요청/수락/거절)

```
ChatController (handleChatRequest / Accept / Reject)
  → webSocketSenderService.sendChatToDevice()
    → /user/{deviceId}/queue/chat  →  상대 디바이스 수신
```

> RabbitMQ를 거치지 않고 직접 전송. 방 생성 전이라 영속화 불필요.

### 스태프 → 디바이스 (제재/알림)

```
StaffChatController (sanctionRoom / notifyDevice)
  → chatRoomService / webSocketSenderService
    → sendChatToDevice()
      → /user/{deviceId}/queue/chat  →  디바이스 수신
```

---

## 6. 파일 구조

```
com.mycompany.tablemaster
│
├─ entity/
│  ├─ ChatRoom.java                    ← 신규
│  ├─ ChatRoomParticipant.java         ← 신규
│  ├─ ChatMessage.java                 ← 신규
│  ├─ ChatReport.java                  ← 신규
│  ├─ StaffChatReadPosition.java       ← 신규
│  ├─ ChatRoomStatus.java              ← 신규 enum
│  ├─ ChatMessageType.java             ← 신규 enum
│  └─ ChatReportStatus.java            ← 신규 enum
│
├─ repository/
│  ├─ ChatRoomRepository.java          ← 신규
│  ├─ ChatRoomParticipantRepository.java ← 신규
│  ├─ ChatMessageRepository.java       ← 신규
│  ├─ ChatReportRepository.java        ← 신규
│  └─ StaffChatReadPositionRepository.java ← 신규
│
├─ service/
│  ├─ ChatRoomService.java             ← 신규
│  ├─ ChatMessageService.java          ← 신규
│  └─ ChatReportService.java           ← 신규
│
├─ controller/
│  ├─ ChatController.java              ← 신규 (STOMP)
│  └─ StaffChatController.java         ← 신규 (REST)
│
├─ dto/chat/
│  ├─ ChatRoomListResponse.java        ← 신규
│  ├─ ChatRoomDetailResponse.java      ← 신규
│  ├─ ChatMessageResponse.java         ← 신규
│  ├─ ChatSanctionRequest.java         ← 신규
│  ├─ ChatMuteRequest.java             ← 신규
│  ├─ ChatNotifyRequest.java           ← 신규
│  ├─ ChatReadRequest.java             ← 신규
│  ├─ ChatReportRequest.java           ← 신규
│  ├─ ChatRequestMessage.java          ← 신규 (STOMP)
│  ├─ ChatAcceptMessage.java           ← 신규 (STOMP)
│  ├─ ChatRejectMessage.java           ← 신규 (STOMP)
│  ├─ ChatSendMessage.java             ← 신규 (STOMP)
│  └─ ChatGiftMessage.java             ← 신규 (STOMP)
│
├─ event/
│  ├─ ChatEvent.java                   ← 수정 (필드 추가)
│  └─ ChatEventType.java               ← 수정 (GIFT 추가)
│
├─ messaging/consumer/
│  └─ ChatEventConsumer.java           ← 수정 (DB저장 + 스태프 토픽)
│
└─ exception/
   └─ BusinessException.java           ← 수정 (CHAT_001~006 추가)
```
