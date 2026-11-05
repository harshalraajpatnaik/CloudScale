package com.cloudscale.ai.dto;

import java.util.List;
import java.util.Map;

public record DiagnoseIncidentResponseDTO(
        String title,
        String rootCause,
        String severity,
        List<String> affectedComponents,
        List<String> remediationSteps,
        Double confidenceScore
) {}
