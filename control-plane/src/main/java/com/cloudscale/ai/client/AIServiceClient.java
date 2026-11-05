package com.cloudscale.ai.client;

import java.util.List;
import java.util.Map;

public interface AIServiceClient {

    Map<String, Object> diagnoseIncident(Map<String, Object> incidentData);

    List<Map<String, Object>> generateRecommendations(Map<String, Object> clusterContext);

    String chat(String message, List<Map<String, String>> history, Map<String, Object> clusterContext);

    Map<String, Object> forecastCapacity(List<Map<String, Object>> history, String metricKey, int horizonSteps);
}
