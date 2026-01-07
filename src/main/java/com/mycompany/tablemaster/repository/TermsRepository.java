package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.Terms;
import com.mycompany.tablemaster.entity.Terms.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermsRepository extends JpaRepository<Terms, Long> {

    List<Terms> findByIsActiveTrue();

    List<Terms> findByIsActiveTrueAndRequiredTrue();

    Optional<Terms> findByTypeAndIsActiveTrue(TermsType type);

    List<Terms> findByTypeOrderByVersionDesc(TermsType type);
}
