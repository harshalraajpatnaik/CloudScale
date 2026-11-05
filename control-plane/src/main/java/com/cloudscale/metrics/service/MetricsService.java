package com.cloudscale.metrics.service;

import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.metrics.dto.ClusterMetricsSummaryDTO;
import com.cloudscale.metrics.dto.IngestNodeMetricsRequest;
import com.cloudscale.metrics.dto.NodeMetricDTO;
import com.cloudscale.metrics.entity.NodeMetric;
import com.cloudscale.metrics.repository.NodeMetricRepository;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final NodeMetricRepository nodeMetricRepository;
    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;

    public MetricsService(NodeMetricRepository nodeMetricRepository, NodeRepository nodeRepository, WorkloadRepository workloadRepository) {
        this.nodeMetricRepository = nodeMetricRepository;
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
    }

    @Transactional
    public NodeMetricDTO ingestNodeMetric(UUID orgId, UUID nodeId, IngestNodeMetricsRequest request) {
        Node node = nodeRepository.findById(nodeId)
                .filter(n -> n.getOrganisation().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND", "Compute node not found in active organisation"));

        Instant sampledAt = request.sampledAt() != null ? request.sampledAt() : Instant.now();

        NodeMetric metric = new NodeMetric(
                node,
                request.cpuUtilizationPercent().setScale(2, RoundingMode.HALF_UP),
                request.memoryUtilizationPercent().setScale(2, RoundingMode.HALF_UP),
                request.diskReadBytesSec(),
                request.diskWriteBytesSec(),
                request.networkRxBytesSec(),
                request.networkTxBytesSec(),
                request.activeWorkloadCount(),
                sampledAt
        );

        NodeMetric saved = nodeMetricRepository.save(metric);

        // Update host heartbeat
        node.setLastHeartbeat(Instant.now());
        if (node.getStatus() == NodeStatus.UNREACHABLE) {
            node.setStatus(NodeStatus.ACTIVE);
            log.info("Node {} received heartbeat; restored state from UNREACHABLE to ACTIVE", node.getHostname());
        }
        nodeRepository.save(node);

        return NodeMetricDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<NodeMetricDTO> getNodeMetrics(UUID orgId, UUID nodeId, int durationMinutes) {
        Node node = nodeRepository.findById(nodeId)
                .filter(n -> n.getOrganisation().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND", "Compute node not found in active organisation"));

        Instant since = Instant.now().minus(Math.max(1, durationMinutes), ChronoUnit.MINUTES);
        return nodeMetricRepository.findByNodeIdSince(node.getId(), since).stream()
                .map(NodeMetricDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClusterMetricsSummaryDTO getClusterSummary(UUID orgId, int durationMinutes) {
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        int totalNodes = nodes.size();
        int activeNodes = (int) nodes.stream().filter(n -> n.getStatus() == NodeStatus.ACTIVE).count();
        int runningWorkloadsCount = workloadRepository.findByOrganisationIdAndActualState(orgId, WorkloadState.RUNNING).size();

        Instant since = Instant.now().minus(Math.max(1, durationMinutes), ChronoUnit.MINUTES);
        List<NodeMetric> recentMetrics = nodeMetricRepository.findByOrganisationIdSince(orgId, since);

        if (recentMetrics.isEmpty()) {
            return new ClusterMetricsSummaryDTO(
                    totalNodes,
                    activeNodes,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    runningWorkloadsCount,
                    since,
                    Instant.now()
            );
        }

        BigDecimal sumCpu = BigDecimal.ZERO;
        BigDecimal peakCpu = BigDecimal.ZERO;
        BigDecimal sumMem = BigDecimal.ZERO;
        BigDecimal peakMem = BigDecimal.ZERO;
        int maxWorkloads = 0;

        for (NodeMetric m : recentMetrics) {
            sumCpu = sumCpu.add(m.getCpuUtilizationPercent());
            if (m.getCpuUtilizationPercent().compareTo(peakCpu) > 0) {
                peakCpu = m.getCpuUtilizationPercent();
            }

            sumMem = sumMem.add(m.getMemoryUtilizationPercent());
            if (m.getMemoryUtilizationPercent().compareTo(peakMem) > 0) {
                peakMem = m.getMemoryUtilizationPercent();
            }

            if (m.getActiveWorkloadCount() > maxWorkloads) {
                maxWorkloads = m.getActiveWorkloadCount();
            }
        }

        BigDecimal count = BigDecimal.valueOf(recentMetrics.size());
        BigDecimal avgCpu = sumCpu.divide(count, 2, RoundingMode.HALF_UP);
        BigDecimal avgMem = sumMem.divide(count, 2, RoundingMode.HALF_UP);
        int totalActiveWorkloads = runningWorkloadsCount > 0 ? runningWorkloadsCount : maxWorkloads;

        return new ClusterMetricsSummaryDTO(
                totalNodes,
                activeNodes,
                avgCpu,
                peakCpu.setScale(2, RoundingMode.HALF_UP),
                avgMem,
                peakMem.setScale(2, RoundingMode.HALF_UP),
                totalActiveWorkloads,
                since,
                Instant.now()
        );
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 02:00 AM
    @Transactional
    public void cleanupOldMetrics() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = nodeMetricRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Metrics retention job purged {} historical samples older than {}", deleted, cutoff);
        }
    }
}
