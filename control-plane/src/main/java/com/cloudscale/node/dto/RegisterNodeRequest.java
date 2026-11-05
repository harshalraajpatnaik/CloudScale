package com.cloudscale.node.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterNodeRequest(
        @NotBlank(message = "Hostname is required")
        @Size(min = 2, max = 100)
        String hostname,

        @NotBlank(message = "IP address is required")
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|localhost|host\\.docker\\.internal", message = "Valid IP address or host endpoint is required")
        String ipAddress,

        @Min(value = 1, message = "Total CPU cores must be at least 1")
        int totalCpuCores,

        @Min(value = 512, message = "Total memory must be at least 512 MB")
        long totalMemoryMb,

        boolean simulated
) {}
