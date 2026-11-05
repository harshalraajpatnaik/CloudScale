package com.cloudscale.node.dto;

import com.cloudscale.node.model.NodeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateNodeStatusRequest(
        @NotNull(message = "Node status is required")
        NodeStatus status
) {}
