package com.cloudscale.cost.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NodeCostDTO(
        UUID nodeId,
        String hostname,
        int allocatedCores,
        int totalCores,
        double allocatedMemoryGb,
        double totalMemoryGb,
        BigDecimal hourlyCostUsd,
        BigDecimal monthlyCostUsd,
        double utilizationPercent,
        boolean isUnderutilized
) {}
