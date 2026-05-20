package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.DeadLetterMessage;
import com.mycompany.tablemaster.entity.DeadLetterMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, Long> {
    List<DeadLetterMessage> findByStatusOrderByLoggedAtDesc(DeadLetterMessageStatus status);

    List<DeadLetterMessage> findByOriginalQueueOrderByLoggedAtDesc(String originalQueue);
}
