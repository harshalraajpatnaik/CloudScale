package com.cloudscale.audit.dto;

import com.cloudscale.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDTO(
        UUID id,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String clientIp,
        Instant recordedAt
) {
    public static AuditLogDTO fromEntity(AuditLog log) {
        return new AuditLogDTO(
                log.getId(),
                log.getActor() != null ? log.getActor().getEmail() : "system",
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getClientIp(),
                log.getRecordedAt()
        );
    }
}
