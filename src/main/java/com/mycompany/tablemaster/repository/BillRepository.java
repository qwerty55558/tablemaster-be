package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.Bill;
import com.mycompany.tablemaster.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByDeviceId(String deviceId);

    List<Bill> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Optional<Bill> findByDeviceIdAndStatus(String deviceId, BillStatus status);

    List<Bill> findByStatus(BillStatus status);

    List<Bill> findByStatusOrderByCreatedAtDesc(BillStatus status);

    List<Bill> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.deviceId = :deviceId AND b.status <> 'CANCELLED'")
    Long sumTotalAmountByDeviceIdExcludingCancelled(@Param("deviceId") String deviceId);
}
