package com.mycompany.tablemaster.messaging;

import com.mycompany.tablemaster.entity.ProcessedMessage;
import com.mycompany.tablemaster.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyGuard {

    private final ProcessedMessageRepository repository;

    public String computeKey(String queueName, byte[] payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(queueName.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0x1f);
            md.update(payload);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public boolean isAlreadyProcessed(String idempotencyKey) {
        return repository.existsByIdempotencyKey(idempotencyKey);
    }

    /**
     * 처리 완료 마킹. UNIQUE 위반 발생 시 동시 처리로 간주하고 false 반환.
     * 비즈니스 트랜잭션이 롤백돼도 마킹은 남도록 REQUIRES_NEW.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(String idempotencyKey, String queueName) {
        try {
            repository.save(ProcessedMessage.builder()
                    .idempotencyKey(idempotencyKey)
                    .queueName(queueName)
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotency key already exists: {}", idempotencyKey);
            return false;
        }
    }
}
