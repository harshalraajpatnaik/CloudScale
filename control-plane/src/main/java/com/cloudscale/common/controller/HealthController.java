package com.cloudscale.common.controller;

import com.cloudscale.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service health and diagnostics endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Liveness probe", description = "Returns operational health status of the control plane")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "cloudscale-control-plane",
                "version", "1.0.0-SNAPSHOT",
                "runtime", "Java " + System.getProperty("java.version")
        );
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
