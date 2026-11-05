package com.cloudscale.alert.dto;

import com.cloudscale.alert.entity.Alert;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record AlertDTO(
        UUID id,
        String severity,
        String alertType,
        String message,
        String status,
        String nodeHostname,
        String workloadName,
        Instant triggeredAt,
        Instant resolvedAt,
        Long durationMinutes
) {
    public static AlertDTO fromEntity(Alert alert) {
        Long duration = null;
        if (alert.getResolvedAt() != null && alert.getTriggeredAt() != null) {
            duration = Duration.between(alert.getTriggeredAt(), alert.getResolvedAt()).toMinutes();
        } else if (alert.getTriggeredAt() != null) {
            duration = Duration.between(alert.getTriggeredAt(), Instant.now()).toMinutes();
        }

        return new AlertDTO(
                alert.getId(),
                alert.getSeverity(),
                alert.getAlertType(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getNode() != null ? alert.getNode().getHostname() : null,
                alert.getWorkload() != null ? alert.getWorkload().getName() : null,
                alert.getTriggeredAt(),
                alert.getResolvedAt(),
                duration
        );
    }
}
