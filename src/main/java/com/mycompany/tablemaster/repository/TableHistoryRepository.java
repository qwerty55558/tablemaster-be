package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.TableHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TableHistoryRepository extends JpaRepository<TableHistory, Long> {

    List<TableHistory> findByDeviceId(String deviceId);

    List<TableHistory> findByName(String name);

    Page<TableHistory> findByDeletedAtAfterOrderByDeletedAtDesc(LocalDateTime after, Pageable pageable);
}
