package com.cloudscale.ai.dto;

import java.util.Map;

public record DiagnoseIncidentRequestDTO(
        Map<String, Object> incidentData
) {}
