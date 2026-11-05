package com.cloudscale.tenancy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisations")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "plan_tier", nullable = false, length = 50)
    private String planTier = "FREE";

    @Column(name = "max_cpu_quota", nullable = false)
    private int maxCpuQuota = 64;

    @Column(name = "max_memory_mb_quota", nullable = false)
    private long maxMemoryMbQuota = 131072L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Organisation() {}

    public Organisation(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public int getMaxCpuQuota() { return maxCpuQuota; }
    public void setMaxCpuQuota(int maxCpuQuota) { this.maxCpuQuota = maxCpuQuota; }

    public long getMaxMemoryMbQuota() { return maxMemoryMbQuota; }
    public void setMaxMemoryMbQuota(long maxMemoryMbQuota) { this.maxMemoryMbQuota = maxMemoryMbQuota; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
