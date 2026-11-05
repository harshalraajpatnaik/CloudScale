package com.cloudscale.ai.controller;

import com.cloudscale.ai.dto.*;
import com.cloudscale.ai.service.AIService;
import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI & Infrastructure Intelligence", description = "LLM-assisted operations, capacity forecasting, and incident diagnostics")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/recommendations")
    @Operation(summary = "List AI recommendations", description = "Retrieves pending and evaluated optimization suggestions")
    public ResponseEntity<ApiResponse<List<AIRecommendationDTO>>> listRecommendations(@AuthenticationPrincipal UserPrincipal principal) {
        List<AIRecommendationDTO> recs = aiService.listRecommendations(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }

    @PostMapping("/recommendations/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Generate AI recommendations", description = "Evaluates cluster allocations and generates rightsizing and rebalancing recommendations")
    public ResponseEntity<ApiResponse<List<AIRecommendationDTO>>> generateRecommendations(@AuthenticationPrincipal UserPrincipal principal) {
        List<AIRecommendationDTO> recs = aiService.generateRecommendations(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }

    @PostMapping("/recommendations/{recId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Approve AI recommendation", description = "Marks recommendation as approved by operator")
    public ResponseEntity<ApiResponse<AIRecommendationDTO>> approveRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID recId
    ) {
        AIRecommendationDTO approved = aiService.approveRecommendation(principal.getOrganisationId(), recId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(approved));
    }

    @PostMapping("/recommendations/{recId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Reject AI recommendation", description = "Rejects suggested recommendation")
    public ResponseEntity<ApiResponse<AIRecommendationDTO>> rejectRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID recId
    ) {
        AIRecommendationDTO rejected = aiService.rejectRecommendation(principal.getOrganisationId(), recId);
        return ResponseEntity.ok(ApiResponse.ok(rejected));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI Infrastructure Assistant", description = "Natural language queries regarding cluster health, saturation, and workloads")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatRequestDTO request
    ) {
        ChatResponseDTO response = aiService.chat(principal.getOrganisationId(), request.message(), request.history());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/diagnose")
    @Operation(summary = "Diagnose incident telemetry", description = "Evaluates telemetry spikes and generates root-cause and remediation steps")
    public ResponseEntity<ApiResponse<DiagnoseIncidentResponseDTO>> diagnoseIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DiagnoseIncidentRequestDTO request
    ) {
        DiagnoseIncidentResponseDTO report = aiService.diagnoseIncident(principal.getOrganisationId(), request.incidentData());
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    @GetMapping("/forecast/nodes/{nodeId}")
    @Operation(summary = "Forecast host capacity", description = "Multi-model ML capacity forecasting comparing Naive, Moving Average, and LightGBM")
    public ResponseEntity<ApiResponse<ForecastResponseDTO>> forecastCapacity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        ForecastResponseDTO forecast = aiService.forecastNodeCapacity(principal.getOrganisationId(), nodeId, durationMinutes);
        return ResponseEntity.ok(ApiResponse.ok(forecast));
    }
}
