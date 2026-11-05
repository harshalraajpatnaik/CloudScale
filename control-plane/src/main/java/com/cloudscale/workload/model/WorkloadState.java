package com.cloudscale.workload.model;

import java.util.Set;

public enum WorkloadState {
    PENDING,
    SCHEDULING,
    RUNNING,
    STOPPING,
    STOPPED,
    SUSPENDED,
    FAILED,
    COMPLETED;

    public boolean canTransitionTo(WorkloadState next) {
        if (this == next) return true;
        return switch (this) {
            case PENDING -> next == SCHEDULING || next == FAILED || next == STOPPED;
            case SCHEDULING -> next == RUNNING || next == FAILED || next == PENDING;
            case RUNNING -> next == STOPPING || next == SUSPENDED || next == FAILED || next == COMPLETED;
            case STOPPING -> next == STOPPED || next == FAILED;
            case STOPPED -> next == PENDING || next == SCHEDULING || next == RUNNING;
            case SUSPENDED -> next == RUNNING || next == STOPPING || next == STOPPED;
            case FAILED -> next == PENDING || next == SCHEDULING;
            case COMPLETED -> false;
        };
    }
}
