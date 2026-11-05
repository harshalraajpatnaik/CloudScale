package com.cloudscale.workload.dto;

import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkloadRequest(
        @NotBlank(message = "Workload name is required")
        @Size(min = 2, max = 100)
        String name,

        @NotNull(message = "Workload type is required")
        WorkloadType workloadType,

        @NotNull(message = "Priority is required")
        WorkloadPriority priority,

        @Min(value = 1, message = "CPU requirement must be at least 1 core")
        int cpuCoresRequired,

        @Min(value = 128, message = "Memory requirement must be at least 128 MB")
        long memoryMbRequired
) {}
