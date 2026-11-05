package com.cloudscale.autoscaling.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScalingEvaluationResult(
        UUID policyId,
        UUID workloadId,
        String decision, // SCALE_UP, SCALE_DOWN, COOLDOWN_ACTIVE, NOOP_WITHIN_BOUNDS, CLAMPED_AT_LIMIT
        int previousReplicas,
        int newReplicas,
        String triggerMetric,
        BigDecimal triggerValue,
        String reasoning
) {}
