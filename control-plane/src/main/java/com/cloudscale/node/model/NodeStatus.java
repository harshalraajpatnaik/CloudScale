package com.cloudscale.node.model;

public enum NodeStatus {
    PENDING,
    ACTIVE,
    DRAINING,
    MAINTENANCE,
    INACTIVE,
    UNREACHABLE;

    public boolean isEligibleForScheduling() {
        return this == ACTIVE;
    }
}
