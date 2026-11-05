package com.cloudscale.autoscaling.controller;

import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.autoscaling.dto.CreateScalingPolicyRequest;
import com.cloudscale.autoscaling.dto.ScalingEvaluationResult;
import com.cloudscale.autoscaling.dto.ScalingEventDTO;
import com.cloudscale.autoscaling.dto.ScalingPolicyDTO;
import com.cloudscale.autoscaling.service.AutoscalingService;
import com.cloudscale.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/autoscaling")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Autoscaling Engine", description = "Horizontal autoscaling policies, autonomous evaluation, and scaling event history")
public class AutoscalingController {

    private final AutoscalingService autoscalingService;

    public AutoscalingController(AutoscalingService autoscalingService) {
        this.autoscalingService = autoscalingService;
    }

    @PostMapping("/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Create scaling policy", description = "Defines CPU/memory scale up and scale down thresholds with cooldown damping")
    public ResponseEntity<ApiResponse<ScalingPolicyDTO>> createPolicy(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateScalingPolicyRequest request
    ) {
        ScalingPolicyDTO policy = autoscalingService.createPolicy(principal.getOrganisationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(policy));
    }

    @GetMapping("/policies")
    @Operation(summary = "List scaling policies", description = "Retrieves all autoscaling policies for the tenant organisation")
    public ResponseEntity<ApiResponse<List<ScalingPolicyDTO>>> listPolicies(@AuthenticationPrincipal UserPrincipal principal) {
        List<ScalingPolicyDTO> policies = autoscalingService.listPolicies(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(policies));
    }

    @GetMapping("/policies/{policyId}")
    @Operation(summary = "Get scaling policy", description = "Inspects threshold definitions, limits, and cooldown status")
    public ResponseEntity<ApiResponse<ScalingPolicyDTO>> getPolicy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID policyId
    ) {
        ScalingPolicyDTO policy = autoscalingService.getPolicy(principal.getOrganisationId(), policyId);
        return ResponseEntity.ok(ApiResponse.ok(policy));
    }

    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Delete scaling policy", description = "Removes the autoscaling policy from the workload")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID policyId
    ) {
        autoscalingService.deletePolicy(principal.getOrganisationId(), policyId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/policies/{policyId}/evaluate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Evaluate policy manually", description = "Executes immediate rule-based threshold evaluation against live or simulated telemetry")
    public ResponseEntity<ApiResponse<ScalingEvaluationResult>> evaluatePolicy(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID policyId,
            @RequestParam(required = false) BigDecimal simulatedCpuUtil
    ) {
        ScalingEvaluationResult result = autoscalingService.evaluatePolicy(principal.getOrganisationId(), policyId, simulatedCpuUtil);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/workloads/{workloadId}/events")
    @Operation(summary = "List scaling events", description = "Audit trail of scaling triggers, previous/new replica counts, and reasoning")
    public ResponseEntity<ApiResponse<List<ScalingEventDTO>>> getScalingEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId
    ) {
        List<ScalingEventDTO> events = autoscalingService.getScalingEvents(principal.getOrganisationId(), workloadId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
