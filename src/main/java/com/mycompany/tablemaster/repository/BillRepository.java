package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.Bill;
import com.mycompany.tablemaster.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByDeviceId(String deviceId);

    Optional<Bill> findByDeviceIdAndStatus(String deviceId, BillStatus status);

    List<Bill> findByStatus(BillStatus status);
}
