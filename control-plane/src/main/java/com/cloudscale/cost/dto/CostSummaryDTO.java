package com.cloudscale.cost.dto;

import java.math.BigDecimal;

public record CostSummaryDTO(
        BigDecimal hourlyBurnRateUsd,
        BigDecimal dailyBurnRateUsd,
        BigDecimal monthlyBurnRateUsd,
        int totalCpuCoresAllocated,
        double totalMemoryGbAllocated,
        int activeWorkloadsCount,
        int totalNodesCount,
        String currency
) {}
