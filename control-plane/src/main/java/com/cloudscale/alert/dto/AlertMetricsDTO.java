package com.cloudscale.alert.dto;

public record AlertMetricsDTO(
        long totalActive,
        long criticalCount,
        long highCount,
        long resolvedPast24Hours,
        double meanTimeToResolutionMinutes
) {}
