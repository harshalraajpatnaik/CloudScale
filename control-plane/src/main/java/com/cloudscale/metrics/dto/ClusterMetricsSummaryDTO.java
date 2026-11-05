package com.cloudscale.metrics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ClusterMetricsSummaryDTO(
        int totalNodes,
        int activeNodes,
        BigDecimal averageCpuUtilizationPercent,
        BigDecimal peakCpuUtilizationPercent,
        BigDecimal averageMemoryUtilizationPercent,
        BigDecimal peakMemoryUtilizationPercent,
        int totalActiveWorkloads,
        Instant windowStartTime,
        Instant windowEndTime
) {}
