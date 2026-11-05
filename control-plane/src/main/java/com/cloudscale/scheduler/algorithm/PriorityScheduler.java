package com.cloudscale.scheduler.algorithm;

import com.cloudscale.node.entity.Node;
import com.cloudscale.workload.entity.Workload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Priority-Aware Scheduling Algorithm
 *
 * Places higher-priority workloads on nodes with higher remaining headroom and reliability.
 *
 * Time Complexity: O(n).
 * Space Complexity: O(1).
 */
@Component("PRIORITY")
public class PriorityScheduler implements SchedulingAlgorithm {

    @Override
    public Optional<Node> selectNode(Workload workload, List<Node> candidates) {
        int reqCpu = workload.getCpuCoresRequired();
        long reqMem = workload.getMemoryMbRequired();

        // Higher headroom preferred for CRITICAL workloads
        return candidates.stream()
                .filter(node -> node.canAccommodate(reqCpu, reqMem))
                .max(Comparator.comparingInt(Node::getAvailableCpuCores));
    }

    @Override
    public String getAlgorithmName() {
        return "PRIORITY";
    }

    @Override
    public String getDescription() {
        return "Headroom-oriented scheduler optimizing high-priority workload resilience.";
    }
}
