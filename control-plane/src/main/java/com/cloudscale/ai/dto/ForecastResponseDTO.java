package com.cloudscale.ai.dto;

import java.util.List;
import java.util.Map;

public record ForecastResponseDTO(
        List<Map<String, Object>> evaluatedModels,
        String selectedModel,
        Map<String, Object> modelPerformance,
        int forecastHorizonMinutes,
        List<Map<String, Object>> forecast
) {}
