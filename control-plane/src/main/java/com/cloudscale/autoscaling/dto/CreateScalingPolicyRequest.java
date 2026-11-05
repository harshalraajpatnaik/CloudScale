package com.cloudscale.autoscaling.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateScalingPolicyRequest(
        @NotNull(message = "Workload ID is required")
        UUID workloadId,

        @NotNull(message = "CPU scale up threshold is required")
        @DecimalMin(value = "10.00")
        @DecimalMax(value = "100.00")
        BigDecimal cpuScaleUpThreshold,

        @NotNull(message = "CPU scale down threshold is required")
        @DecimalMin(value = "5.00")
        @DecimalMax(value = "90.00")
        BigDecimal cpuScaleDownThreshold,

        @DecimalMin(value = "10.00")
        @DecimalMax(value = "100.00")
        BigDecimal memoryScaleUpThreshold,

        @DecimalMin(value = "5.00")
        @DecimalMax(value = "90.00")
        BigDecimal memoryScaleDownThreshold,

        @Min(value = 1, message = "Min replicas must be at least 1")
        int minReplicas,

        @Min(value = 1, message = "Max replicas must be at least 1")
        int maxReplicas,

        @Min(value = 10, message = "Cooldown must be at least 10 seconds")
        int cooldownSeconds,

        @Min(value = 30, message = "Evaluation window must be at least 30 seconds")
        int evaluationWindowSeconds
) {}
