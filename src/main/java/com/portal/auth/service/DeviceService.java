package com.portal.auth.service;

import com.portal.auth.dto.response.DeviceResponse;
import com.portal.auth.exception.AuthException;
import com.portal.auth.model.Device;
import com.portal.auth.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final long activeWindowMinutes;
    private final int maxActiveDevices;

    public DeviceService(DeviceRepository deviceRepository,
                         @Value("${app.device.active-window-minutes}") long activeWindowMinutes,
                         @Value("${app.device.max-active-devices}") int maxActiveDevices) {
        this.deviceRepository = deviceRepository;
        this.activeWindowMinutes = activeWindowMinutes;
        this.maxActiveDevices = maxActiveDevices;
    }

    public void checkAndEnforceDeviceLimit(String userId) {
        var cutoff = LocalDateTime.now().minusMinutes(activeWindowMinutes);
        long activeCount = deviceRepository.countByUserIdAndLastActiveAtAfter(userId, cutoff);

        if (activeCount >= maxActiveDevices) {
            var oldest = deviceRepository.findByUserIdAndLastActiveAtAfter(userId, cutoff)
                    .stream()
                    .sorted((a, b) -> a.getLastActiveAt().compareTo(b.getLastActiveAt()))
                    .findFirst();
            oldest.ifPresent(d -> deviceRepository.delete(d));
        }
    }

    public Device registerDevice(String userId, String deviceId, String deviceTypeStr, String deviceName) {
        var existing = deviceRepository.findByUserIdAndDeviceId(userId, deviceId);
        if (existing.isPresent()) {
            var device = existing.get();
            device.setLastActiveAt(LocalDateTime.now());
            return deviceRepository.save(device);
        }

        Device.DeviceType type;
        try {
            type = Device.DeviceType.valueOf(deviceTypeStr);
        } catch (IllegalArgumentException e) {
            type = Device.DeviceType.web;
        }

        var device = new Device();
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setDeviceType(type);
        device.setName(deviceName != null ? deviceName : deviceTypeStr);
        return deviceRepository.save(device);
    }

    public void updateHeartbeat(String userId, String deviceId) {
        var device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new AuthException("AUTH_400", "Device not found", "deviceId"));
        device.setLastActiveAt(LocalDateTime.now());
        deviceRepository.save(device);
    }

    public List<DeviceResponse> listDevices(String userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void removeDevice(String userId, String deviceId) {
        deviceRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    private DeviceResponse toResponse(Device device) {
        var resp = new DeviceResponse();
        resp.setId(device.getId());
        resp.setDeviceId(device.getDeviceId());
        resp.setDeviceType(device.getDeviceType().name());
        resp.setName(device.getName());
        resp.setLastActiveAt(device.getLastActiveAt());
        resp.setCreatedAt(device.getCreatedAt());
        return resp;
    }
}
