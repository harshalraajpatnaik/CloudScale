package com.cloudscale.cost.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkloadCostDTO(
        UUID workloadId,
        String name,
        String assignedNode,
        int cpuCores,
        double memoryGb,
        BigDecimal hourlyCostUsd,
        BigDecimal monthlyCostUsd,
        String priority,
        String state
) {}
