package com.cloudscale.autoscaling.entity;

import com.cloudscale.workload.entity.Workload;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scaling_events")
public class ScalingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scaling_policy_id", nullable = false)
    private ScalingPolicy scalingPolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_id", nullable = false)
    private Workload workload;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // SCALE_UP, SCALE_DOWN, NOOP

    @Column(name = "previous_replicas", nullable = false)
    private int previousReplicas;

    @Column(name = "new_replicas", nullable = false)
    private int newReplicas;

    @Column(name = "trigger_metric", nullable = false, length = 50)
    private String triggerMetric;

    @Column(name = "trigger_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal triggerValue;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    public ScalingEvent() {}

    public ScalingEvent(ScalingPolicy scalingPolicy, Workload workload, String actionType,
                        int previousReplicas, int newReplicas,
                        String triggerMetric, BigDecimal triggerValue, String reasoning) {
        this.scalingPolicy = scalingPolicy;
        this.workload = workload;
        this.actionType = actionType;
        this.previousReplicas = previousReplicas;
        this.newReplicas = newReplicas;
        this.triggerMetric = triggerMetric;
        this.triggerValue = triggerValue;
        this.reasoning = reasoning;
        this.executedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ScalingPolicy getScalingPolicy() { return scalingPolicy; }
    public void setScalingPolicy(ScalingPolicy scalingPolicy) { this.scalingPolicy = scalingPolicy; }

    public Workload getWorkload() { return workload; }
    public void setWorkload(Workload workload) { this.workload = workload; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public int getPreviousReplicas() { return previousReplicas; }
    public void setPreviousReplicas(int previousReplicas) { this.previousReplicas = previousReplicas; }

    public int getNewReplicas() { return newReplicas; }
    public void setNewReplicas(int newReplicas) { this.newReplicas = newReplicas; }

    public String getTriggerMetric() { return triggerMetric; }
    public void setTriggerMetric(String triggerMetric) { this.triggerMetric = triggerMetric; }

    public BigDecimal getTriggerValue() { return triggerValue; }
    public void setTriggerValue(BigDecimal triggerValue) { this.triggerValue = triggerValue; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
