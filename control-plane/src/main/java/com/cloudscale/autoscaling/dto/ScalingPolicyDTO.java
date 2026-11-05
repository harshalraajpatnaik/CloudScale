package com.cloudscale.autoscaling.dto;

import com.cloudscale.autoscaling.entity.ScalingPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScalingPolicyDTO(
        UUID id,
        UUID organisationId,
        UUID workloadId,
        String workloadName,
        BigDecimal cpuScaleUpThreshold,
        BigDecimal cpuScaleDownThreshold,
        BigDecimal memoryScaleUpThreshold,
        BigDecimal memoryScaleDownThreshold,
        int minReplicas,
        int maxReplicas,
        int cooldownSeconds,
        int evaluationWindowSeconds,
        boolean isEnabled,
        Instant lastScaledAt,
        Instant createdAt
) {
    public static ScalingPolicyDTO fromEntity(ScalingPolicy p) {
        return new ScalingPolicyDTO(
                p.getId(),
                p.getOrganisation().getId(),
                p.getWorkload().getId(),
                p.getWorkload().getName(),
                p.getCpuScaleUpThreshold(),
                p.getCpuScaleDownThreshold(),
                p.getMemoryScaleUpThreshold(),
                p.getMemoryScaleDownThreshold(),
                p.getMinReplicas(),
                p.getMaxReplicas(),
                p.getCooldownSeconds(),
                p.getEvaluationWindowSeconds(),
                p.isEnabled(),
                p.getLastScaledAt(),
                p.getCreatedAt()
        );
    }
}
