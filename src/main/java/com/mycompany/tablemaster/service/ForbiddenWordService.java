package com.mycompany.tablemaster.service;

import com.mycompany.tablemaster.dto.chat.ForbiddenWordCreateRequest;
import com.mycompany.tablemaster.dto.chat.ForbiddenWordResponse;
import com.mycompany.tablemaster.dto.chat.ForbiddenWordUpdateRequest;
import com.mycompany.tablemaster.entity.ForbiddenWord;
import com.mycompany.tablemaster.entity.User;
import com.mycompany.tablemaster.exception.BusinessException;
import com.mycompany.tablemaster.repository.ForbiddenWordRepository;
import com.mycompany.tablemaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForbiddenWordService {

    private final ForbiddenWordRepository forbiddenWordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ForbiddenWordResponse> getForbiddenWords() {
        return forbiddenWordRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ForbiddenWordResponse createForbiddenWord(ForbiddenWordCreateRequest request, Long userId) {
        String normalizedWord = normalizeWord(request.getWord());
        forbiddenWordRepository.findByWordIgnoreCase(normalizedWord).ifPresent(existing -> {
            throw BusinessException.forbiddenWordAlreadyExists();
        });

        User user = userRepository.findById(userId)
                .orElseThrow(BusinessException::userNotFound);

        ForbiddenWord forbiddenWord = ForbiddenWord.builder()
                .word(normalizedWord)
                .reason(request.getReason())
                .createdByUserId(userId)
                .createdByName(user.getName())
                .build();

        ForbiddenWord saved = forbiddenWordRepository.save(forbiddenWord);
        log.info("Forbidden word created: word={}, by={}", normalizedWord, userId);
        return toResponse(saved);
    }

    @Transactional
    public ForbiddenWordResponse updateForbiddenWord(Long id, ForbiddenWordUpdateRequest request) {
        ForbiddenWord forbiddenWord = forbiddenWordRepository.findById(id)
                .orElseThrow(BusinessException::forbiddenWordNotFound);

        if (request.getWord() != null && !request.getWord().isBlank()) {
            String normalizedWord = normalizeWord(request.getWord());
            forbiddenWordRepository.findByWordIgnoreCase(normalizedWord)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw BusinessException.forbiddenWordAlreadyExists();
                    });
            forbiddenWord.setWord(normalizedWord);
        }

        if (request.getReason() != null) {
            forbiddenWord.setReason(request.getReason());
        }

        if (request.getIsActive() != null) {
            forbiddenWord.setIsActive(request.getIsActive());
        }

        ForbiddenWord saved = forbiddenWordRepository.save(forbiddenWord);
        log.info("Forbidden word updated: id={}", id);
        return toResponse(saved);
    }

    @Transactional
    public void deleteForbiddenWord(Long id) {
        if (!forbiddenWordRepository.existsById(id)) {
            throw BusinessException.forbiddenWordNotFound();
        }
        forbiddenWordRepository.deleteById(id);
        log.info("Forbidden word deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public void validateMessage(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        String normalizedContent = content.toLowerCase(Locale.ROOT);
        forbiddenWordRepository.findByIsActiveTrueOrderByWordAsc().stream()
                .filter(word -> normalizedContent.contains(word.getWord().toLowerCase(Locale.ROOT)))
                .findFirst()
                .ifPresent(word -> {
                    throw BusinessException.forbiddenWordDetected(word.getWord());
                });
    }

    private String normalizeWord(String word) {
        return word.trim().toLowerCase(Locale.ROOT);
    }

    private ForbiddenWordResponse toResponse(ForbiddenWord forbiddenWord) {
        String createdByName = userRepository.findById(forbiddenWord.getCreatedByUserId())
                .map(User::getName)
                .orElse(forbiddenWord.getCreatedByName());

        return ForbiddenWordResponse.builder()
                .id(forbiddenWord.getId())
                .word(forbiddenWord.getWord())
                .reason(forbiddenWord.getReason())
                .isActive(forbiddenWord.getIsActive())
                .createdByUserId(forbiddenWord.getCreatedByUserId())
                .createdByName(createdByName)
                .createdAt(forbiddenWord.getCreatedAt())
                .updatedAt(forbiddenWord.getUpdatedAt())
                .build();
    }
}
