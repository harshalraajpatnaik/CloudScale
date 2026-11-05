package com.cloudscale.workload.model;

public enum WorkloadPriority {
    CRITICAL(5),
    HIGH(4),
    NORMAL(3),
    LOW(2),
    BATCH(1);

    private final int weight;

    WorkloadPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
