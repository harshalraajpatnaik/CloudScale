package com.cloudscale.autoscaling.dto;

import com.cloudscale.autoscaling.entity.ScalingEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScalingEventDTO(
        UUID id,
        UUID scalingPolicyId,
        UUID workloadId,
        String workloadName,
        String actionType,
        int previousReplicas,
        int newReplicas,
        String triggerMetric,
        BigDecimal triggerValue,
        String reasoning,
        Instant executedAt
) {
    public static ScalingEventDTO fromEntity(ScalingEvent e) {
        return new ScalingEventDTO(
                e.getId(),
                e.getScalingPolicy().getId(),
                e.getWorkload().getId(),
                e.getWorkload().getName(),
                e.getActionType(),
                e.getPreviousReplicas(),
                e.getNewReplicas(),
                e.getTriggerMetric(),
                e.getTriggerValue(),
                e.getReasoning(),
                e.getExecutedAt()
        );
    }
}
