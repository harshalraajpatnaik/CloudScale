package com.cloudscale.workload.entity;

import com.cloudscale.node.entity.Node;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.model.WorkloadType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workloads")
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_node_id")
    private Node assignedNode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload_type", nullable = false, length = 50)
    private WorkloadType workloadType = WorkloadType.SERVICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkloadPriority priority = WorkloadPriority.NORMAL;

    @Column(name = "cpu_cores_required", nullable = false)
    private int cpuCoresRequired;

    @Column(name = "memory_mb_required", nullable = false)
    private long memoryMbRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_state", nullable = false, length = 50)
    private WorkloadState desiredState = WorkloadState.RUNNING;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_state", nullable = false, length = 50)
    private WorkloadState actualState = WorkloadState.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "scheduling_algorithm", length = 50)
    private String schedulingAlgorithm = "RESOURCE_AWARE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Workload() {}

    public Workload(Organisation organisation, String name, WorkloadType workloadType, WorkloadPriority priority, int cpuCoresRequired, long memoryMbRequired) {
        this.organisation = organisation;
        this.name = name;
        this.workloadType = workloadType;
        this.priority = priority;
        this.cpuCoresRequired = cpuCoresRequired;
        this.memoryMbRequired = memoryMbRequired;
        this.desiredState = WorkloadState.RUNNING;
        this.actualState = WorkloadState.PENDING;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organisation getOrganisation() { return organisation; }
    public void setOrganisation(Organisation organisation) { this.organisation = organisation; }

    public Node getAssignedNode() { return assignedNode; }
    public void setAssignedNode(Node assignedNode) { this.assignedNode = assignedNode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public WorkloadType getWorkloadType() { return workloadType; }
    public void setWorkloadType(WorkloadType workloadType) { this.workloadType = workloadType; }

    public WorkloadPriority getPriority() { return priority; }
    public void setPriority(WorkloadPriority priority) { this.priority = priority; }

    public int getCpuCoresRequired() { return cpuCoresRequired; }
    public void setCpuCoresRequired(int cpuCoresRequired) { this.cpuCoresRequired = cpuCoresRequired; }

    public long getMemoryMbRequired() { return memoryMbRequired; }
    public void setMemoryMbRequired(long memoryMbRequired) { this.memoryMbRequired = memoryMbRequired; }

    public WorkloadState getDesiredState() { return desiredState; }
    public void setDesiredState(WorkloadState desiredState) { this.desiredState = desiredState; }

    public WorkloadState getActualState() { return actualState; }
    public void setActualState(WorkloadState actualState) { this.actualState = actualState; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getSchedulingAlgorithm() { return schedulingAlgorithm; }
    public void setSchedulingAlgorithm(String schedulingAlgorithm) { this.schedulingAlgorithm = schedulingAlgorithm; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void transitionTo(WorkloadState newState) {
        if (!this.actualState.canTransitionTo(newState)) {
            throw new IllegalStateException("Illegal workload state transition from " + this.actualState + " to " + newState);
        }
        this.actualState = newState;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
