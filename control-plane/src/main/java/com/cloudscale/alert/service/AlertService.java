package com.cloudscale.alert.service;

import com.cloudscale.alert.dto.AlertDTO;
import com.cloudscale.alert.dto.AlertMetricsDTO;
import com.cloudscale.alert.entity.Alert;
import com.cloudscale.alert.repository.AlertRepository;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final OrganisationRepository organisationRepository;
    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;

    public AlertService(
            AlertRepository alertRepository,
            OrganisationRepository organisationRepository,
            NodeRepository nodeRepository,
            WorkloadRepository workloadRepository
    ) {
        this.alertRepository = alertRepository;
        this.organisationRepository = organisationRepository;
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
    }

    @Transactional
    public AlertDTO createAlert(UUID orgId, UUID nodeId, UUID workloadId, String severity, String alertType, String message) {
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        Node node = nodeId != null ? nodeRepository.findById(nodeId).orElse(null) : null;
        Workload workload = workloadId != null ? workloadRepository.findById(workloadId).orElse(null) : null;

        Alert alert = new Alert(org, node, workload, severity, alertType, message);
        Alert saved = alertRepository.save(alert);
        log.warn("Created {} alert for organisation {}: {}", severity, org.getName(), message);
        return AlertDTO.fromEntity(saved);
    }

    @Transactional
    public AlertDTO resolveAlert(UUID orgId, UUID alertId) {
        Alert alert = alertRepository.findByIdAndOrganisationId(alertId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ALERT_NOT_FOUND", "Alert not found"));

        alert.setStatus("RESOLVED");
        alert.setResolvedAt(Instant.now());
        Alert saved = alertRepository.save(alert);
        log.info("Resolved alert {} for organisation {}", alertId, orgId);
        return AlertDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> listAlerts(UUID orgId, String status) {
        List<Alert> alerts;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            alerts = alertRepository.findByOrganisationIdAndStatusOrderByTriggeredAtDesc(orgId, status.toUpperCase());
        } else {
            alerts = alertRepository.findByOrganisationIdOrderByTriggeredAtDesc(orgId);
        }
        return alerts.stream().map(AlertDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public AlertMetricsDTO getMetrics(UUID orgId) {
        List<Alert> allAlerts = alertRepository.findByOrganisationIdOrderByTriggeredAtDesc(orgId);

        long activeCount = 0;
        long criticalCount = 0;
        long highCount = 0;
        long resolved24h = 0;
        long totalResolutionMinutes = 0;
        long resolvedCount = 0;

        Instant past24h = Instant.now().minus(24, ChronoUnit.HOURS);

        for (Alert a : allAlerts) {
            if ("ACTIVE".equalsIgnoreCase(a.getStatus())) {
                activeCount++;
                if ("CRITICAL".equalsIgnoreCase(a.getSeverity())) {
                    criticalCount++;
                } else if ("HIGH".equalsIgnoreCase(a.getSeverity())) {
                    highCount++;
                }
            } else if ("RESOLVED".equalsIgnoreCase(a.getStatus()) && a.getResolvedAt() != null) {
                resolvedCount++;
                totalResolutionMinutes += Duration.between(a.getTriggeredAt(), a.getResolvedAt()).toMinutes();
                if (a.getResolvedAt().isAfter(past24h)) {
                    resolved24h++;
                }
            }
        }

        double mttr = resolvedCount > 0 ? (double) totalResolutionMinutes / resolvedCount : 0.0;

        return new AlertMetricsDTO(
                activeCount,
                criticalCount,
                highCount,
                resolved24h,
                Math.round(mttr * 10.0) / 10.0
        );
    }
}
