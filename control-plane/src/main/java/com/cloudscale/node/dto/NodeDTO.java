package com.cloudscale.node.dto;

import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import java.time.Instant;
import java.util.UUID;

public record NodeDTO(
        UUID id,
        UUID organisationId,
        String hostname,
        String ipAddress,
        int totalCpuCores,
        long totalMemoryMb,
        int allocatedCpuCores,
        long allocatedMemoryMb,
        int availableCpuCores,
        long availableMemoryMb,
        double cpuUtilizationPercent,
        double memoryUtilizationPercent,
        NodeStatus status,
        boolean isSimulated,
        Instant lastHeartbeat,
        Instant createdAt
) {
    public static NodeDTO fromEntity(Node node) {
        double cpuUtil = node.getTotalCpuCores() > 0 
                ? ((double) node.getAllocatedCpuCores() / node.getTotalCpuCores()) * 100.0 
                : 0.0;
        double memUtil = node.getTotalMemoryMb() > 0 
                ? ((double) node.getAllocatedMemoryMb() / node.getTotalMemoryMb()) * 100.0 
                : 0.0;

        return new NodeDTO(
                node.getId(),
                node.getOrganisation().getId(),
                node.getHostname(),
                node.getIpAddress(),
                node.getTotalCpuCores(),
                node.getTotalMemoryMb(),
                node.getAllocatedCpuCores(),
                node.getAllocatedMemoryMb(),
                node.getAvailableCpuCores(),
                node.getAvailableMemoryMb(),
                Math.round(cpuUtil * 100.0) / 100.0,
                Math.round(memUtil * 100.0) / 100.0,
                node.getStatus(),
                node.isSimulated(),
                node.getLastHeartbeat(),
                node.getCreatedAt()
        );
    }
}
