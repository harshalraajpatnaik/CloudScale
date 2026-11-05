package com.cloudscale.node.entity;

import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.tenancy.entity.Organisation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nodes")
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(nullable = false, unique = true, length = 100)
    private String hostname;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "total_cpu_cores", nullable = false)
    private int totalCpuCores;

    @Column(name = "total_memory_mb", nullable = false)
    private long totalMemoryMb;

    @Column(name = "allocated_cpu_cores", nullable = false)
    private int allocatedCpuCores = 0;

    @Column(name = "allocated_memory_mb", nullable = false)
    private long allocatedMemoryMb = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NodeStatus status = NodeStatus.ACTIVE;

    @Column(name = "is_simulated", nullable = false)
    private boolean simulated = true;

    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Node() {}

    public Node(Organisation organisation, String hostname, String ipAddress, int totalCpuCores, long totalMemoryMb, boolean simulated) {
        this.organisation = organisation;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.totalCpuCores = totalCpuCores;
        this.totalMemoryMb = totalMemoryMb;
        this.simulated = simulated;
        this.status = NodeStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organisation getOrganisation() { return organisation; }
    public void setOrganisation(Organisation organisation) { this.organisation = organisation; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getTotalCpuCores() { return totalCpuCores; }
    public void setTotalCpuCores(int totalCpuCores) { this.totalCpuCores = totalCpuCores; }

    public long getTotalMemoryMb() { return totalMemoryMb; }
    public void setTotalMemoryMb(long totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }

    public int getAllocatedCpuCores() { return allocatedCpuCores; }
    public void setAllocatedCpuCores(int allocatedCpuCores) { this.allocatedCpuCores = allocatedCpuCores; }

    public long getAllocatedMemoryMb() { return allocatedMemoryMb; }
    public void setAllocatedMemoryMb(long allocatedMemoryMb) { this.allocatedMemoryMb = allocatedMemoryMb; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public int getAvailableCpuCores() {
        return Math.max(0, totalCpuCores - allocatedCpuCores);
    }

    public long getAvailableMemoryMb() {
        return Math.max(0L, totalMemoryMb - allocatedMemoryMb);
    }

    public boolean canAccommodate(int cpuRequired, long memoryMbRequired) {
        return status == NodeStatus.ACTIVE 
                && getAvailableCpuCores() >= cpuRequired 
                && getAvailableMemoryMb() >= memoryMbRequired;
    }

    public void allocate(int cpuRequired, long memoryMbRequired) {
        if (!canAccommodate(cpuRequired, memoryMbRequired)) {
            throw new IllegalStateException("Node " + hostname + " cannot accommodate " + cpuRequired + " CPU / " + memoryMbRequired + "MB");
        }
        this.allocatedCpuCores += cpuRequired;
        this.allocatedMemoryMb += memoryMbRequired;
    }

    public void release(int cpuRequired, long memoryMbRequired) {
        this.allocatedCpuCores = Math.max(0, this.allocatedCpuCores - cpuRequired);
        this.allocatedMemoryMb = Math.max(0L, this.allocatedMemoryMb - memoryMbRequired);
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
