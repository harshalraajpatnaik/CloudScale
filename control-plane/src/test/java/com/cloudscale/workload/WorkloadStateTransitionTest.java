package com.cloudscale.workload;

import com.cloudscale.workload.model.WorkloadState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadStateTransitionTest {

    @Test
    @DisplayName("Valid transitions from PENDING state")
    void testPendingTransitions() {
        assertThat(WorkloadState.PENDING.canTransitionTo(WorkloadState.SCHEDULING)).isTrue();
        assertThat(WorkloadState.PENDING.canTransitionTo(WorkloadState.FAILED)).isTrue();
        assertThat(WorkloadState.PENDING.canTransitionTo(WorkloadState.STOPPED)).isTrue();
        // Illegal jump
        assertThat(WorkloadState.PENDING.canTransitionTo(WorkloadState.RUNNING)).isFalse();
        assertThat(WorkloadState.PENDING.canTransitionTo(WorkloadState.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("Valid transitions from SCHEDULING state")
    void testSchedulingTransitions() {
        assertThat(WorkloadState.SCHEDULING.canTransitionTo(WorkloadState.RUNNING)).isTrue();
        assertThat(WorkloadState.SCHEDULING.canTransitionTo(WorkloadState.FAILED)).isTrue();
        assertThat(WorkloadState.SCHEDULING.canTransitionTo(WorkloadState.PENDING)).isTrue();
        // Illegal jump
        assertThat(WorkloadState.SCHEDULING.canTransitionTo(WorkloadState.COMPLETED)).isFalse();
        assertThat(WorkloadState.SCHEDULING.canTransitionTo(WorkloadState.STOPPED)).isFalse();
    }

    @Test
    @DisplayName("Valid transitions from RUNNING state")
    void testRunningTransitions() {
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.STOPPING)).isTrue();
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.SUSPENDED)).isTrue();
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.FAILED)).isTrue();
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.COMPLETED)).isTrue();
        // Illegal jump without stopping
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.STOPPED)).isFalse();
        assertThat(WorkloadState.RUNNING.canTransitionTo(WorkloadState.PENDING)).isFalse();
    }

    @Test
    @DisplayName("Terminal COMPLETED state allows no transitions")
    void testCompletedTransitions() {
        assertThat(WorkloadState.COMPLETED.canTransitionTo(WorkloadState.RUNNING)).isFalse();
        assertThat(WorkloadState.COMPLETED.canTransitionTo(WorkloadState.PENDING)).isFalse();
        assertThat(WorkloadState.COMPLETED.canTransitionTo(WorkloadState.SCHEDULING)).isFalse();
    }

    @Test
    @DisplayName("Self transitions are always allowed")
    void testSelfTransitions() {
        for (WorkloadState state : WorkloadState.values()) {
            assertThat(state.canTransitionTo(state)).isTrue();
        }
    }
}
