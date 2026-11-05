package com.cloudscale.audit.service;

import com.cloudscale.audit.dto.AuditLogDTO;
import com.cloudscale.audit.entity.AuditLog;
import com.cloudscale.audit.repository.AuditLogRepository;
import com.cloudscale.auth.entity.User;
import com.cloudscale.auth.repository.UserRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            OrganisationRepository organisationRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordEvent(
            UUID orgId,
            UUID actorUserId,
            String action,
            String resourceType,
            String resourceId,
            String previousState,
            String newState,
            String clientIp
    ) {
        try {
            Organisation org = organisationRepository.findById(orgId).orElse(null);
            if (org == null) return;

            User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

            AuditLog entry = new AuditLog(
                    org,
                    actor,
                    action,
                    resourceType,
                    resourceId,
                    previousState,
                    newState,
                    clientIp != null ? clientIp : "127.0.0.1"
            );

            auditLogRepository.save(entry);
            log.info("Audit: [{}] {} on {} ({}) by {}", org.getName(), action, resourceType, resourceId, actor != null ? actor.getEmail() : "system");
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getRecentLogs(UUID orgId, int limit) {
        int max = Math.min(Math.max(1, limit), 100);
        return auditLogRepository.findByOrganisationIdOrderByRecordedAtDesc(orgId, PageRequest.of(0, max))
                .stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }
}
