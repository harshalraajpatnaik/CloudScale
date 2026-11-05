package com.cloudscale.scheduler;

import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.scheduler.algorithm.BestFitScheduler;
import com.cloudscale.scheduler.algorithm.FirstFitScheduler;
import com.cloudscale.scheduler.algorithm.PriorityScheduler;
import com.cloudscale.scheduler.algorithm.ResourceAwareScheduler;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingAlgorithmsTest {

    private ResourceAwareScheduler resourceAwareScheduler;
    private BestFitScheduler bestFitScheduler;
    private FirstFitScheduler firstFitScheduler;
    private PriorityScheduler priorityScheduler;
    private Organisation org;

    @BeforeEach
    void setUp() {
        resourceAwareScheduler = new ResourceAwareScheduler();
        bestFitScheduler = new BestFitScheduler();
        firstFitScheduler = new FirstFitScheduler();
        priorityScheduler = new PriorityScheduler();
        org = new Organisation("Test Org", "test-org");
    }

    private Node createNode(String hostname, int totalCpu, long totalMem, int allocCpu, long allocMem) {
        Node node = new Node(org, hostname, "10.0.0.1", totalCpu, totalMem, true);
        node.setAllocatedCpuCores(allocCpu);
        node.setAllocatedMemoryMb(allocMem);
        node.setStatus(NodeStatus.ACTIVE);
        return node;
    }

    private Workload createWorkload(int reqCpu, long reqMem, WorkloadPriority priority) {
        return new Workload(org, "test-workload", WorkloadType.SERVICE, priority, reqCpu, reqMem);
    }

    @Test
    @DisplayName("Resource-Aware Scheduler balances dual CPU and Memory utilization ratios")
    void testResourceAwareSchedulerBalancing() {
        // Node 1: 16 CPU, 32GB RAM, allocated 8 CPU (50%) and 8GB RAM (25%) -> unbalanced
        Node node1 = createNode("node-1", 16, 32768, 8, 8192);
        // Node 2: 16 CPU, 32GB RAM, allocated 4 CPU (25%) and 8GB RAM (25%) -> balanced
        Node node2 = createNode("node-2", 16, 32768, 4, 8192);

        // Workload needs 4 CPU and 8GB RAM (25% CPU, 25% RAM)
        // If placed on Node 1: future CPU = 12/16 (75%), future Mem = 16384/32768 (50%) -> diff = 0.25
        // If placed on Node 2: future CPU = 8/16 (50%), future Mem = 16384/32768 (50%) -> diff = 0.00 (perfect balance!)
        Workload workload = createWorkload(4, 8192, WorkloadPriority.NORMAL);

        Optional<Node> selected = resourceAwareScheduler.selectNode(workload, List.of(node1, node2));

        assertThat(selected).isPresent();
        assertThat(selected.get().getHostname()).isEqualTo("node-2");
    }

    @Test
    @DisplayName("Best-Fit Scheduler selects the node with minimal leftover capacity to pack tightly")
    void testBestFitSchedulerPacking() {
        // Node 1 has 12 CPU, 24GB remaining
        Node node1 = createNode("node-loose", 16, 32768, 4, 8192);
        // Node 2 has 3 CPU, 6GB remaining
        Node node2 = createNode("node-tight", 8, 16384, 5, 10240);

        Workload workload = createWorkload(2, 4096, WorkloadPriority.NORMAL);

        Optional<Node> selected = bestFitScheduler.selectNode(workload, List.of(node1, node2));

        assertThat(selected).isPresent();
        assertThat(selected.get().getHostname()).isEqualTo("node-tight");
    }

    @Test
    @DisplayName("First-Fit Scheduler selects the first qualifying candidate in sequence")
    void testFirstFitSchedulerFastSelection() {
        Node node1 = createNode("node-alpha", 8, 16384, 0, 0);
        Node node2 = createNode("node-beta", 16, 32768, 0, 0);

        Workload workload = createWorkload(2, 2048, WorkloadPriority.NORMAL);

        Optional<Node> selected = firstFitScheduler.selectNode(workload, List.of(node1, node2));

        assertThat(selected).isPresent();
        assertThat(selected.get().getHostname()).isEqualTo("node-alpha");
    }

    @Test
    @DisplayName("Priority Scheduler chooses the node with greatest CPU headroom for critical workloads")
    void testPrioritySchedulerHeadroom() {
        Node nodeLowHeadroom = createNode("node-low", 8, 16384, 5, 10240); // 3 CPU available
        Node nodeHighHeadroom = createNode("node-high", 16, 32768, 4, 8192); // 12 CPU available

        Workload workload = createWorkload(2, 4096, WorkloadPriority.CRITICAL);

        Optional<Node> selected = priorityScheduler.selectNode(workload, List.of(nodeLowHeadroom, nodeHighHeadroom));

        assertThat(selected).isPresent();
        assertThat(selected.get().getHostname()).isEqualTo("node-high");
    }

    @Test
    @DisplayName("All schedulers return empty when no node has sufficient CPU or memory")
    void testInsufficientCapacityReturnsEmpty() {
        Node smallNode = createNode("node-tiny", 2, 4096, 1, 2048); // Only 1 CPU, 2GB available
        Workload hugeWorkload = createWorkload(8, 16384, WorkloadPriority.CRITICAL); // Needs 8 CPU, 16GB

        assertThat(resourceAwareScheduler.selectNode(hugeWorkload, List.of(smallNode))).isEmpty();
        assertThat(bestFitScheduler.selectNode(hugeWorkload, List.of(smallNode))).isEmpty();
        assertThat(firstFitScheduler.selectNode(hugeWorkload, List.of(smallNode))).isEmpty();
        assertThat(priorityScheduler.selectNode(hugeWorkload, List.of(smallNode))).isEmpty();
    }
}
