package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, String> {

    List<TableEntity> findByStatus(TableStatus status);

    List<TableEntity> findByLocation(String location);

    /**
     * 테이블명으로 조회 (채팅 요청 시 상대 테이블 찾기용)
     * name은 중복 가능하므로 status가 OCCUPIED인 것만 조회
     */
    Optional<TableEntity> findByNameAndStatus(String name, TableStatus status);

    /**
     * 테이블명으로 조회 (모든 상태)
     */
    List<TableEntity> findByName(String name);

    /**
     * 특정 상태가 아닌 테이블 조회 (AVAILABLE 제외용)
     */
    List<TableEntity> findByStatusNot(TableStatus status);

    /**
     * 여러 상태를 제외한 테이블 조회 (AVAILABLE, INACTIVE 제외용)
     */
    List<TableEntity> findByStatusNotIn(List<TableStatus> statuses);
}
