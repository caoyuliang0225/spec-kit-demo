package com.portal.auth.repository;

import com.portal.auth.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByUserId(String userId);
    Optional<Device> findByUserIdAndDeviceId(String userId, String deviceId);
    List<Device> findByUserIdAndLastActiveAtAfter(String userId, LocalDateTime after);
    long countByUserIdAndLastActiveAtAfter(String userId, LocalDateTime after);
    void deleteByUserIdAndDeviceId(String userId, String deviceId);
}
