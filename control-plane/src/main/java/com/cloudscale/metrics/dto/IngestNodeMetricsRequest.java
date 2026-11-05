package com.cloudscale.metrics.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record IngestNodeMetricsRequest(
        @NotNull(message = "CPU utilization is required")
        @DecimalMin(value = "0.00", message = "CPU utilization must be at least 0.00%")
        @DecimalMax(value = "100.00", message = "CPU utilization must not exceed 100.00%")
        BigDecimal cpuUtilizationPercent,

        @NotNull(message = "Memory utilization is required")
        @DecimalMin(value = "0.00", message = "Memory utilization must be at least 0.00%")
        @DecimalMax(value = "100.00", message = "Memory utilization must not exceed 100.00%")
        BigDecimal memoryUtilizationPercent,

        long diskReadBytesSec,
        long diskWriteBytesSec,
        long networkRxBytesSec,
        long networkTxBytesSec,
        int activeWorkloadCount,
        Instant sampledAt
) {}
