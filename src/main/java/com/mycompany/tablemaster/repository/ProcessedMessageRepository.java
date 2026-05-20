package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
