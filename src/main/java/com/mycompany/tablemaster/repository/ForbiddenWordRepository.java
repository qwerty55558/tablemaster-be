package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    List<ForbiddenWord> findAllByOrderByCreatedAtDesc();

    List<ForbiddenWord> findByIsActiveTrueOrderByWordAsc();

    Optional<ForbiddenWord> findByWordIgnoreCase(String word);
}
