package com.cloudscale.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record ChatRequestDTO(
        @NotBlank(message = "Message prompt is required")
        String message,
        List<Map<String, String>> history
) {}
