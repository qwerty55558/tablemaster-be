package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long messageId);

    Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    List<ChatMessage> findTop50ByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    void deleteByChatRoomId(Long chatRoomId);
}
