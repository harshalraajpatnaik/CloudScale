package com.cloudscale.workload.controller;

import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.scheduler.service.SchedulerService;
import com.cloudscale.workload.dto.CreateWorkloadRequest;
import com.cloudscale.workload.dto.ScheduleWorkloadRequest;
import com.cloudscale.workload.dto.WorkloadDTO;
import com.cloudscale.workload.service.WorkloadService;
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
@RequestMapping("/api/v1/workloads")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Workload Orchestration", description = "Workload lifecycle, state transitions, and deterministic scheduling")
public class WorkloadController {

    private final WorkloadService workloadService;
    private final SchedulerService schedulerService;

    public WorkloadController(WorkloadService workloadService, SchedulerService schedulerService) {
        this.workloadService = workloadService;
        this.schedulerService = schedulerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Submit new workload", description = "Submits a new compute workload specification in PENDING state")
    public ResponseEntity<ApiResponse<WorkloadDTO>> createWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWorkloadRequest request
    ) {
        WorkloadDTO workload = workloadService.createWorkload(principal.getOrganisationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(workload));
    }

    @GetMapping
    @Operation(summary = "List workloads", description = "Retrieves all workloads and their current state machine status")
    public ResponseEntity<ApiResponse<List<WorkloadDTO>>> listWorkloads(@AuthenticationPrincipal UserPrincipal principal) {
        List<WorkloadDTO> workloads = workloadService.listWorkloads(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(workloads));
    }

    @GetMapping("/{workloadId}")
    @Operation(summary = "Get workload details", description = "Inspects allocated resources, host node assignment, and execution status")
    public ResponseEntity<ApiResponse<WorkloadDTO>> getWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId
    ) {
        WorkloadDTO workload = workloadService.getWorkload(principal.getOrganisationId(), workloadId);
        return ResponseEntity.ok(ApiResponse.ok(workload));
    }

    @PostMapping("/{workloadId}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Schedule workload onto cluster", description = "Applies scheduling algorithm with pessimistic concurrency locking")
    public ResponseEntity<ApiResponse<WorkloadDTO>> scheduleWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId,
            @RequestBody(required = false) ScheduleWorkloadRequest request
    ) {
        String algo = request != null ? request.algorithm() : null;
        WorkloadDTO scheduled = schedulerService.schedule(principal.getOrganisationId(), workloadId, algo);
        return ResponseEntity.ok(ApiResponse.ok(scheduled));
    }

    @PostMapping("/{workloadId}/stop")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Stop running workload", description = "Gracefully releases allocated CPU and memory from host node")
    public ResponseEntity<ApiResponse<WorkloadDTO>> stopWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId
    ) {
        WorkloadDTO stopped = workloadService.stopWorkload(principal.getOrganisationId(), workloadId);
        return ResponseEntity.ok(ApiResponse.ok(stopped));
    }

    @PostMapping("/{workloadId}/restart")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Restart workload", description = "Releases existing host allocation and reschedules onto an optimal node")
    public ResponseEntity<ApiResponse<WorkloadDTO>> restartWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId,
            @RequestBody(required = false) ScheduleWorkloadRequest request
    ) {
        String algo = request != null ? request.algorithm() : null;
        WorkloadDTO restarted = workloadService.restartWorkload(principal.getOrganisationId(), workloadId, algo);
        return ResponseEntity.ok(ApiResponse.ok(restarted));
    }

    @PostMapping("/{workloadId}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Suspend workload", description = "Temporarily freezes workload execution")
    public ResponseEntity<ApiResponse<WorkloadDTO>> suspendWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId
    ) {
        WorkloadDTO suspended = workloadService.suspendWorkload(principal.getOrganisationId(), workloadId);
        return ResponseEntity.ok(ApiResponse.ok(suspended));
    }

    @DeleteMapping("/{workloadId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN')")
    @Operation(summary = "Delete workload", description = "Terminates execution, releases node resources, and purges workload")
    public ResponseEntity<ApiResponse<Void>> deleteWorkload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID workloadId
    ) {
        workloadService.deleteWorkload(principal.getOrganisationId(), workloadId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
