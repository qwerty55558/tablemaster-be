package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.UserTermsAgreement;
import com.mycompany.tablemaster.entity.Terms.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {

    List<UserTermsAgreement> findByUserId(Long userId);

    @Query("SELECT uta FROM UserTermsAgreement uta WHERE uta.user.id = :userId AND uta.terms.type = :type ORDER BY uta.agreedAt DESC")
    Optional<UserTermsAgreement> findLatestByUserIdAndTermsType(@Param("userId") Long userId, @Param("type") TermsType type);

    boolean existsByUserIdAndTermsId(Long userId, Long termsId);
}
