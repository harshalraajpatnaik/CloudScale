package com.cloudscale.workload.dto;

import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.model.WorkloadType;
import java.time.Instant;
import java.util.UUID;

public record WorkloadDTO(
        UUID id,
        UUID organisationId,
        UUID assignedNodeId,
        String assignedNodeHostname,
        String name,
        WorkloadType workloadType,
        WorkloadPriority priority,
        int cpuCoresRequired,
        long memoryMbRequired,
        WorkloadState desiredState,
        WorkloadState actualState,
        String failureReason,
        Instant scheduledAt,
        Instant createdAt
) {
    public static WorkloadDTO fromEntity(Workload w) {
        return new WorkloadDTO(
                w.getId(),
                w.getOrganisation().getId(),
                w.getAssignedNode() != null ? w.getAssignedNode().getId() : null,
                w.getAssignedNode() != null ? w.getAssignedNode().getHostname() : null,
                w.getName(),
                w.getWorkloadType(),
                w.getPriority(),
                w.getCpuCoresRequired(),
                w.getMemoryMbRequired(),
                w.getDesiredState(),
                w.getActualState(),
                w.getFailureReason(),
                w.getScheduledAt(),
                w.getCreatedAt()
        );
    }
}
