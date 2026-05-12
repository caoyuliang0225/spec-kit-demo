package com.portal.auth.controller;

import com.portal.auth.dto.response.DeviceResponse;
import com.portal.auth.service.DeviceService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody String deviceId) {
        String userId = getCurrentUserId();
        deviceService.updateHeartbeat(userId, deviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> listDevices() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(deviceService.listDevices(userId));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> removeDevice(@PathVariable String deviceId) {
        String userId = getCurrentUserId();
        deviceService.removeDevice(userId, deviceId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Claims claims) {
            return claims.getSubject();
        }
        return auth != null ? auth.getName() : null;
    }
}
