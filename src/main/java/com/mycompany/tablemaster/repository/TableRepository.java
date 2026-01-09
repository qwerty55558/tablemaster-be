package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.TableEntity;
import com.mycompany.tablemaster.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, String> {

    Optional<TableEntity> findByDeviceId(String deviceId);

    List<TableEntity> findByStatus(TableStatus status);

    List<TableEntity> findByLocation(String location);

    boolean existsByDeviceId(String deviceId);
}
