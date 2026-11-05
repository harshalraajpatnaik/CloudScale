package com.cloudscale.alert.controller;

import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.alert.dto.AlertDTO;
import com.cloudscale.alert.dto.AlertMetricsDTO;
import com.cloudscale.alert.service.AlertService;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final OrganisationRepository organisationRepository;
    private final NodeRepository nodeRepository;

    public AlertController(
            AlertService alertService,
            OrganisationRepository organisationRepository,
            NodeRepository nodeRepository
    ) {
        this.alertService = alertService;
        this.organisationRepository = organisationRepository;
        this.nodeRepository = nodeRepository;
    }

    private UUID getTenantOrgId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getOrganisationId();
        }
        return organisationRepository.findAll().stream()
                .findFirst()
                .map(Organisation::getId)
                .orElse(UUID.randomUUID());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDTO>>> listAlerts(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Authentication auth
    ) {
        UUID orgId = getTenantOrgId(auth);
        List<AlertDTO> alerts = alertService.listAlerts(orgId, status);
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<AlertMetricsDTO>> getMetrics(Authentication auth) {
        UUID orgId = getTenantOrgId(auth);
        AlertMetricsDTO metrics = alertService.getMetrics(orgId);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertDTO>> resolveAlert(
            @PathVariable UUID id,
            Authentication auth
    ) {
        UUID orgId = getTenantOrgId(auth);
        AlertDTO resolved = alertService.resolveAlert(orgId, id);
        return ResponseEntity.ok(ApiResponse.ok(resolved));
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<AlertDTO>> simulateAlert(
            @RequestBody(required = false) Map<String, String> payload,
            Authentication auth
    ) {
        UUID orgId = getTenantOrgId(auth);
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        UUID nodeId = !nodes.isEmpty() ? nodes.get(0).getId() : null;

        String severity = payload != null && payload.containsKey("severity") ? payload.get("severity") : "HIGH";
        String alertType = payload != null && payload.containsKey("alertType") ? payload.get("alertType") : "HOST_SATURATION";
        String message = payload != null && payload.containsKey("message")
                ? payload.get("message")
                : "Host CPU utilization sustained above 90% threshold for >3 consecutive evaluation windows.";

        AlertDTO alert = alertService.createAlert(orgId, nodeId, null, severity, alertType, message);
        return ResponseEntity.ok(ApiResponse.ok(alert));
    }
}
