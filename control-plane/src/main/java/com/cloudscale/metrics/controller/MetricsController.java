package com.cloudscale.metrics.controller;

import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.metrics.dto.ClusterMetricsSummaryDTO;
import com.cloudscale.metrics.dto.IngestNodeMetricsRequest;
import com.cloudscale.metrics.dto.NodeMetricDTO;
import com.cloudscale.metrics.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Telemetry & Metrics", description = "Time-series resource utilization ingestion and historical analysis")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Ingest node telemetry", description = "Pushes CPU, memory, disk, and network telemetry sample for a compute host")
    public ResponseEntity<ApiResponse<NodeMetricDTO>> ingestNodeMetric(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId,
            @Valid @RequestBody IngestNodeMetricsRequest request
    ) {
        NodeMetricDTO dto = metricsService.ingestNodeMetric(principal.getOrganisationId(), nodeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dto));
    }

    @GetMapping("/nodes/{nodeId}")
    @Operation(summary = "Query node metrics history", description = "Retrieves time-series telemetry samples for a host over a time window")
    public ResponseEntity<ApiResponse<List<NodeMetricDTO>>> getNodeMetrics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        List<NodeMetricDTO> metrics = metricsService.getNodeMetrics(principal.getOrganisationId(), nodeId, durationMinutes);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }

    @GetMapping("/cluster/summary")
    @Operation(summary = "Get cluster metrics summary", description = "Aggregates average and peak CPU/memory saturation across all hosts")
    public ResponseEntity<ApiResponse<ClusterMetricsSummaryDTO>> getClusterSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        ClusterMetricsSummaryDTO summary = metricsService.getClusterSummary(principal.getOrganisationId(), durationMinutes);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
