package com.mycompany.tablemaster.dto.sync;

import com.mycompany.tablemaster.dto.notification.NotificationDTO;
import com.mycompany.tablemaster.dto.table.TableListResponse;
import com.mycompany.tablemaster.dto.table.TableSetupResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SyncResponse {
    private List<TableListResponse> tables;     // 전체 테이블 목록 (싱크용)
    private TableSetupResponse table;           // 내 테이블 상태
    private List<NotificationDTO> notifications;
    private LocalDateTime timestamp;
}
