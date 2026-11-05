package com.cloudscale.scheduler.algorithm;

import com.cloudscale.node.entity.Node;
import com.cloudscale.workload.entity.Workload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Best-Fit Scheduling Algorithm
 *
 * Maximizes node consolidation by packing the workload onto the eligible host
 * with the smallest remaining leftover capacity after placement.
 *
 * Time Complexity: O(n) where n is the number of active eligible nodes.
 * Space Complexity: O(1).
 */
@Component("BEST_FIT")
public class BestFitScheduler implements SchedulingAlgorithm {

    @Override
    public Optional<Node> selectNode(Workload workload, List<Node> candidates) {
        int reqCpu = workload.getCpuCoresRequired();
        long reqMem = workload.getMemoryMbRequired();

        return candidates.stream()
                .filter(node -> node.canAccommodate(reqCpu, reqMem))
                .min(Comparator.comparingDouble(node -> calculateRemainingLeftover(node, reqCpu, reqMem)));
    }

    private double calculateRemainingLeftover(Node node, int reqCpu, long reqMem) {
        double remCpu = node.getAvailableCpuCores() - reqCpu;
        double remMemGb = (double) (node.getAvailableMemoryMb() - reqMem) / 1024.0;
        return remCpu + remMemGb;
    }

    @Override
    public String getAlgorithmName() {
        return "BEST_FIT";
    }

    @Override
    public String getDescription() {
        return "Consolidation scheduler packing workloads into nodes with minimal leftover capacity.";
    }
}
