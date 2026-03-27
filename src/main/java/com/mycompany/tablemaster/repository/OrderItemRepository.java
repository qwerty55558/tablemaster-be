package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByBillId(Long billId);

    Optional<OrderItem> findByIdAndBillDeviceId(Long id, String deviceId);
}
