package com.cloudscale.alert;

import com.cloudscale.alert.dto.AlertDTO;
import com.cloudscale.alert.dto.AlertMetricsDTO;
import com.cloudscale.alert.entity.Alert;
import com.cloudscale.alert.repository.AlertRepository;
import com.cloudscale.alert.service.AlertService;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertWorkflowTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private WorkloadRepository workloadRepository;

    @InjectMocks
    private AlertService alertService;

    private UUID orgId;
    private Organisation org;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        org = new Organisation("Incident Org", "incident-org");
        org.setId(orgId);
    }

    @Test
    @DisplayName("Create alert stores record with ACTIVE status")
    void testCreateAlert() {
        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AlertDTO alert = alertService.createAlert(orgId, null, null, "CRITICAL", "NODE_OOM", "Worker node running out of memory");

        assertThat(alert.status()).isEqualTo("ACTIVE");
        assertThat(alert.severity()).isEqualTo("CRITICAL");
        assertThat(alert.alertType()).isEqualTo("NODE_OOM");
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("Resolve alert updates status to RESOLVED and records resolution timestamp")
    void testResolveAlert() {
        UUID alertId = UUID.randomUUID();
        Alert alert = new Alert(org, null, null, "HIGH", "CPU_SPIKE", "CPU > 90%");
        alert.setId(alertId);

        when(alertRepository.findByIdAndOrganisationId(alertId, orgId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertDTO resolved = alertService.resolveAlert(orgId, alertId);

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.resolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Compute MTTR metrics accurately across resolved incident history")
    void testCalculateMTTR() {
        Instant now = Instant.now();

        Alert active1 = new Alert(org, null, null, "CRITICAL", "NODE_OFFLINE", "Unreachable");
        active1.setStatus("ACTIVE");

        Alert resolved1 = new Alert(org, null, null, "HIGH", "MEMORY_HIGH", "Mem > 85%");
        resolved1.setStatus("RESOLVED");
        resolved1.setTriggeredAt(now.minus(40, ChronoUnit.MINUTES));
        resolved1.setResolvedAt(now.minus(10, ChronoUnit.MINUTES)); // Took 30 mins

        Alert resolved2 = new Alert(org, null, null, "MEDIUM", "DISK_FULL", "Disk > 90%");
        resolved2.setStatus("RESOLVED");
        resolved2.setTriggeredAt(now.minus(20, ChronoUnit.MINUTES));
        resolved2.setResolvedAt(now.minus(10, ChronoUnit.MINUTES)); // Took 10 mins

        when(alertRepository.findByOrganisationIdOrderByTriggeredAtDesc(orgId))
                .thenReturn(List.of(active1, resolved1, resolved2));

        AlertMetricsDTO metrics = alertService.getMetrics(orgId);

        assertThat(metrics.totalActive()).isEqualTo(1);
        assertThat(metrics.criticalCount()).isEqualTo(1);
        assertThat(metrics.resolvedPast24Hours()).isEqualTo(2);
        // MTTR = (30 + 10) / 2 = 20.0 minutes
        assertThat(metrics.meanTimeToResolutionMinutes()).isEqualTo(20.0);
    }
}
