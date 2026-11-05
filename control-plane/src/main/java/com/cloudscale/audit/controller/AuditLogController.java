package com.cloudscale.audit.controller;

import com.cloudscale.audit.dto.AuditLogDTO;
import com.cloudscale.audit.service.AuditLogService;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final OrganisationRepository organisationRepository;

    public AuditLogController(AuditLogService auditLogService, OrganisationRepository organisationRepository) {
        this.auditLogService = auditLogService;
        this.organisationRepository = organisationRepository;
    }

    private UUID getTenantOrgId(Authentication auth) {
        return organisationRepository.findAll().stream()
                .findFirst()
                .map(Organisation::getId)
                .orElse(UUID.randomUUID());
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getRecentLogs(
            @RequestParam(required = false, defaultValue = "50") int limit,
            Authentication auth
    ) {
        UUID orgId = getTenantOrgId(auth);
        List<AuditLogDTO> logs = auditLogService.getRecentLogs(orgId, limit);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
