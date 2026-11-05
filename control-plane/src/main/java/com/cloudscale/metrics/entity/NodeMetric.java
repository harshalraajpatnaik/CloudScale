package com.cloudscale.metrics.entity;

import com.cloudscale.node.entity.Node;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "node_metrics")
public class NodeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(name = "cpu_utilization_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cpuUtilizationPercent;

    @Column(name = "memory_utilization_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal memoryUtilizationPercent;

    @Column(name = "disk_read_bytes_sec", nullable = false)
    private long diskReadBytesSec = 0L;

    @Column(name = "disk_write_bytes_sec", nullable = false)
    private long diskWriteBytesSec = 0L;

    @Column(name = "network_rx_bytes_sec", nullable = false)
    private long networkRxBytesSec = 0L;

    @Column(name = "network_tx_bytes_sec", nullable = false)
    private long networkTxBytesSec = 0L;

    @Column(name = "active_workload_count", nullable = false)
    private int activeWorkloadCount = 0;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt = Instant.now();

    public NodeMetric() {}

    public NodeMetric(Node node, BigDecimal cpuUtilizationPercent, BigDecimal memoryUtilizationPercent,
                      long diskReadBytesSec, long diskWriteBytesSec,
                      long networkRxBytesSec, long networkTxBytesSec,
                      int activeWorkloadCount, Instant sampledAt) {
        this.node = node;
        this.cpuUtilizationPercent = cpuUtilizationPercent;
        this.memoryUtilizationPercent = memoryUtilizationPercent;
        this.diskReadBytesSec = diskReadBytesSec;
        this.diskWriteBytesSec = diskWriteBytesSec;
        this.networkRxBytesSec = networkRxBytesSec;
        this.networkTxBytesSec = networkTxBytesSec;
        this.activeWorkloadCount = activeWorkloadCount;
        this.sampledAt = sampledAt != null ? sampledAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Node getNode() { return node; }
    public void setNode(Node node) { this.node = node; }

    public BigDecimal getCpuUtilizationPercent() { return cpuUtilizationPercent; }
    public void setCpuUtilizationPercent(BigDecimal cpuUtilizationPercent) { this.cpuUtilizationPercent = cpuUtilizationPercent; }

    public BigDecimal getMemoryUtilizationPercent() { return memoryUtilizationPercent; }
    public void setMemoryUtilizationPercent(BigDecimal memoryUtilizationPercent) { this.memoryUtilizationPercent = memoryUtilizationPercent; }

    public long getDiskReadBytesSec() { return diskReadBytesSec; }
    public void setDiskReadBytesSec(long diskReadBytesSec) { this.diskReadBytesSec = diskReadBytesSec; }

    public long getDiskWriteBytesSec() { return diskWriteBytesSec; }
    public void setDiskWriteBytesSec(long diskWriteBytesSec) { this.diskWriteBytesSec = diskWriteBytesSec; }

    public long getNetworkRxBytesSec() { return networkRxBytesSec; }
    public void setNetworkRxBytesSec(long networkRxBytesSec) { this.networkRxBytesSec = networkRxBytesSec; }

    public long getNetworkTxBytesSec() { return networkTxBytesSec; }
    public void setNetworkTxBytesSec(long networkTxBytesSec) { this.networkTxBytesSec = networkTxBytesSec; }

    public int getActiveWorkloadCount() { return activeWorkloadCount; }
    public void setActiveWorkloadCount(int activeWorkloadCount) { this.activeWorkloadCount = activeWorkloadCount; }

    public Instant getSampledAt() { return sampledAt; }
    public void setSampledAt(Instant sampledAt) { this.sampledAt = sampledAt; }
}
