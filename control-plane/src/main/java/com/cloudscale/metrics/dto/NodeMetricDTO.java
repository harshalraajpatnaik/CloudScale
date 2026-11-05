package com.cloudscale.metrics.dto;

import com.cloudscale.metrics.entity.NodeMetric;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NodeMetricDTO(
        UUID id,
        UUID nodeId,
        String hostname,
        BigDecimal cpuUtilizationPercent,
        BigDecimal memoryUtilizationPercent,
        long diskReadBytesSec,
        long diskWriteBytesSec,
        long networkRxBytesSec,
        long networkTxBytesSec,
        int activeWorkloadCount,
        Instant sampledAt
) {
    public static NodeMetricDTO fromEntity(NodeMetric m) {
        return new NodeMetricDTO(
                m.getId(),
                m.getNode().getId(),
                m.getNode().getHostname(),
                m.getCpuUtilizationPercent(),
                m.getMemoryUtilizationPercent(),
                m.getDiskReadBytesSec(),
                m.getDiskWriteBytesSec(),
                m.getNetworkRxBytesSec(),
                m.getNetworkTxBytesSec(),
                m.getActiveWorkloadCount(),
                m.getSampledAt()
        );
    }
}
