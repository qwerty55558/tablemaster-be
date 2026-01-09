package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.DeviceWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceWhitelistRepository extends JpaRepository<DeviceWhitelist, Long> {
    Optional<DeviceWhitelist> findByDeviceId(String deviceId);
    Optional<DeviceWhitelist> findByDeviceIdAndIsActiveTrue(String deviceId);
    boolean existsByDeviceId(String deviceId);
}
