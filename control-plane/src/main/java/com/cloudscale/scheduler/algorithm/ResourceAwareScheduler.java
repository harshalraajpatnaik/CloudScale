package com.cloudscale.scheduler.algorithm;

import com.cloudscale.node.entity.Node;
import com.cloudscale.workload.entity.Workload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resource-Aware Scheduling Algorithm (Default)
 *
 * Evaluates dual CPU and memory utilization vectors. Selects the eligible host that
 * minimizes variance between post-allocation CPU and memory saturation ratios,
 * preventing stranded capacity and balancing host consumption.
 *
 * Time Complexity: O(n) where n is the number of active eligible nodes.
 * Space Complexity: O(1).
 */
@Component("RESOURCE_AWARE")
public class ResourceAwareScheduler implements SchedulingAlgorithm {

    @Override
    public Optional<Node> selectNode(Workload workload, List<Node> candidates) {
        int reqCpu = workload.getCpuCoresRequired();
        long reqMem = workload.getMemoryMbRequired();

        return candidates.stream()
                .filter(node -> node.canAccommodate(reqCpu, reqMem))
                .min(Comparator.comparingDouble(node -> calculateBalanceScore(node, reqCpu, reqMem)));
    }

    private double calculateBalanceScore(Node node, int reqCpu, long reqMem) {
        double futureCpuRatio = (double) (node.getAllocatedCpuCores() + reqCpu) / node.getTotalCpuCores();
        double futureMemRatio = (double) (node.getAllocatedMemoryMb() + reqMem) / node.getTotalMemoryMb();
        
        // Variance: lower is better (means CPU and Memory are equally utilized)
        double diff = futureCpuRatio - futureMemRatio;
        return (diff * diff) + (0.1 * futureCpuRatio); // tie-breaker slightly favoring lower overall CPU
    }

    @Override
    public String getAlgorithmName() {
        return "RESOURCE_AWARE";
    }

    @Override
    public String getDescription() {
        return "Multi-dimensional Resource-Aware Scheduler minimizing CPU and memory utilization variance.";
    }
}
