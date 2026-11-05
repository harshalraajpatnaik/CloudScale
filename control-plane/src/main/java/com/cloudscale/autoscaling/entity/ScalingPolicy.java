package com.cloudscale.autoscaling.entity;

import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.workload.entity.Workload;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scaling_policies")
public class ScalingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_id", nullable = false, unique = true)
    private Workload workload;

    @Column(name = "cpu_scale_up_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal cpuScaleUpThreshold = new BigDecimal("80.00");

    @Column(name = "cpu_scale_down_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal cpuScaleDownThreshold = new BigDecimal("30.00");

    @Column(name = "memory_scale_up_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal memoryScaleUpThreshold = new BigDecimal("85.00");

    @Column(name = "memory_scale_down_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal memoryScaleDownThreshold = new BigDecimal("35.00");

    @Column(name = "min_replicas", nullable = false)
    private int minReplicas = 1;

    @Column(name = "max_replicas", nullable = false)
    private int maxReplicas = 10;

    @Column(name = "cooldown_seconds", nullable = false)
    private int cooldownSeconds = 300;

    @Column(name = "evaluation_window_seconds", nullable = false)
    private int evaluationWindowSeconds = 300;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_scaled_at")
    private Instant lastScaledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ScalingPolicy() {}

    public ScalingPolicy(Organisation organisation, Workload workload,
                         BigDecimal cpuScaleUpThreshold, BigDecimal cpuScaleDownThreshold,
                         BigDecimal memoryScaleUpThreshold, BigDecimal memoryScaleDownThreshold,
                         int minReplicas, int maxReplicas,
                         int cooldownSeconds, int evaluationWindowSeconds) {
        this.organisation = organisation;
        this.workload = workload;
        this.cpuScaleUpThreshold = cpuScaleUpThreshold;
        this.cpuScaleDownThreshold = cpuScaleDownThreshold;
        this.memoryScaleUpThreshold = memoryScaleUpThreshold;
        this.memoryScaleDownThreshold = memoryScaleDownThreshold;
        this.minReplicas = minReplicas;
        this.maxReplicas = maxReplicas;
        this.cooldownSeconds = cooldownSeconds;
        this.evaluationWindowSeconds = evaluationWindowSeconds;
        this.enabled = true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organisation getOrganisation() { return organisation; }
    public void setOrganisation(Organisation organisation) { this.organisation = organisation; }

    public Workload getWorkload() { return workload; }
    public void setWorkload(Workload workload) { this.workload = workload; }

    public BigDecimal getCpuScaleUpThreshold() { return cpuScaleUpThreshold; }
    public void setCpuScaleUpThreshold(BigDecimal cpuScaleUpThreshold) { this.cpuScaleUpThreshold = cpuScaleUpThreshold; }

    public BigDecimal getCpuScaleDownThreshold() { return cpuScaleDownThreshold; }
    public void setCpuScaleDownThreshold(BigDecimal cpuScaleDownThreshold) { this.cpuScaleDownThreshold = cpuScaleDownThreshold; }

    public BigDecimal getMemoryScaleUpThreshold() { return memoryScaleUpThreshold; }
    public void setMemoryScaleUpThreshold(BigDecimal memoryScaleUpThreshold) { this.memoryScaleUpThreshold = memoryScaleUpThreshold; }

    public BigDecimal getMemoryScaleDownThreshold() { return memoryScaleDownThreshold; }
    public void setMemoryScaleDownThreshold(BigDecimal memoryScaleDownThreshold) { this.memoryScaleDownThreshold = memoryScaleDownThreshold; }

    public int getMinReplicas() { return minReplicas; }
    public void setMinReplicas(int minReplicas) { this.minReplicas = minReplicas; }

    public int getMaxReplicas() { return maxReplicas; }
    public void setMaxReplicas(int maxReplicas) { this.maxReplicas = maxReplicas; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public int getEvaluationWindowSeconds() { return evaluationWindowSeconds; }
    public void setEvaluationWindowSeconds(int evaluationWindowSeconds) { this.evaluationWindowSeconds = evaluationWindowSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getLastScaledAt() { return lastScaledAt; }
    public void setLastScaledAt(Instant lastScaledAt) { this.lastScaledAt = lastScaledAt; }

    public Instant getCreatedAt() { return createdAt; }

    public boolean isInCooldown(Instant now) {
        if (lastScaledAt == null) return false;
        return now.isBefore(lastScaledAt.plusSeconds(cooldownSeconds));
    }
}
