package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.GiftType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GiftTypeRepository extends JpaRepository<GiftType, Long> {
    Optional<GiftType> findByCode(String code);
}
