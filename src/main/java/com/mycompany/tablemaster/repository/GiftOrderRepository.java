package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.GiftOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftOrderRepository extends JpaRepository<GiftOrder, Long> {
    List<GiftOrder> findByBillId(Long billId);
}
