package com.cloudscale.cost.dto;

import java.math.BigDecimal;
import java.util.List;

public record WasteAnalysisDTO(
        BigDecimal monthlyWastedSpendUsd,
        int idleCoresCount,
        double idleMemoryGb,
        int underutilizedNodesCount,
        double potentialSavingsPercent,
        List<String> recommendations
) {}
