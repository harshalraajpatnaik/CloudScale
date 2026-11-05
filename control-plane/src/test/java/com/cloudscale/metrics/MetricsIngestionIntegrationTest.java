package com.cloudscale.metrics;

import com.cloudscale.metrics.dto.ClusterMetricsSummaryDTO;
import com.cloudscale.metrics.dto.IngestNodeMetricsRequest;
import com.cloudscale.metrics.dto.NodeMetricDTO;
import com.cloudscale.metrics.service.MetricsService;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MetricsIngestionIntegrationTest {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    @DisplayName("Metrics ingestion persists samples and calculates accurate cluster aggregates")
    void testMetricsIngestionAndAggregation() {
        String slug = "metrics-test-" + UUID.randomUUID().toString().substring(0, 8);
        Organisation org = organisationRepository.save(new Organisation("Metrics Test Org", slug));
        UUID orgId = org.getId();

        Node node = new Node(org, "metrics-worker-" + slug, "192.168.1.50", 16, 32768L, true);
        node.setStatus(NodeStatus.ACTIVE);
        node = nodeRepository.save(node);
        UUID nodeId = node.getId();

        // Sample 1: 40% CPU, 50% RAM
        IngestNodeMetricsRequest req1 = new IngestNodeMetricsRequest(
                new BigDecimal("40.00"),
                new BigDecimal("50.00"),
                1024L, 2048L, 4096L, 8192L,
                2,
                Instant.now().minusSeconds(120)
        );
        metricsService.ingestNodeMetric(orgId, nodeId, req1);

        // Sample 2: 80% CPU, 70% RAM
        IngestNodeMetricsRequest req2 = new IngestNodeMetricsRequest(
                new BigDecimal("80.00"),
                new BigDecimal("70.00"),
                2048L, 4096L, 8192L, 16384L,
                4,
                Instant.now().minusSeconds(60)
        );
        metricsService.ingestNodeMetric(orgId, nodeId, req2);

        // 1. Query Node Metrics History
        List<NodeMetricDTO> history = metricsService.getNodeMetrics(orgId, nodeId, 60);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).cpuUtilizationPercent()).isEqualByComparingTo("40.00");
        assertThat(history.get(1).cpuUtilizationPercent()).isEqualByComparingTo("80.00");

        // 2. Query Cluster Summary
        ClusterMetricsSummaryDTO summary = metricsService.getClusterSummary(orgId, 60);
        assertThat(summary.totalNodes()).isEqualTo(1);
        assertThat(summary.activeNodes()).isEqualTo(1);
        // Average CPU = (40 + 80) / 2 = 60.00%
        assertThat(summary.averageCpuUtilizationPercent()).isEqualByComparingTo("60.00");
        // Peak CPU = 80.00%
        assertThat(summary.peakCpuUtilizationPercent()).isEqualByComparingTo("80.00");
        // Average Memory = (50 + 70) / 2 = 60.00%
        assertThat(summary.averageMemoryUtilizationPercent()).isEqualByComparingTo("60.00");
        // Peak Memory = 70.00%
        assertThat(summary.peakMemoryUtilizationPercent()).isEqualByComparingTo("70.00");
        // Max active workloads = 4
        assertThat(summary.totalActiveWorkloads()).isEqualTo(4);
    }
}
