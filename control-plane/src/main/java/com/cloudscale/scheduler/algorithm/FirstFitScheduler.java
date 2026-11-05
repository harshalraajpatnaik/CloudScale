package com.cloudscale.scheduler.algorithm;

import com.cloudscale.node.entity.Node;
import com.cloudscale.workload.entity.Workload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * First-Fit Scheduling Algorithm
 *
 * Fast placement strategy assigning the workload to the first node that meets capacity requirements.
 *
 * Time Complexity: O(n) worst-case, O(1) best-case.
 * Space Complexity: O(1).
 */
@Component("FIRST_FIT")
public class FirstFitScheduler implements SchedulingAlgorithm {

    @Override
    public Optional<Node> selectNode(Workload workload, List<Node> candidates) {
        int reqCpu = workload.getCpuCoresRequired();
        long reqMem = workload.getMemoryMbRequired();

        return candidates.stream()
                .filter(node -> node.canAccommodate(reqCpu, reqMem))
                .findFirst();
    }

    @Override
    public String getAlgorithmName() {
        return "FIRST_FIT";
    }

    @Override
    public String getDescription() {
        return "Low-latency scheduling assigning workload to the first eligible host.";
    }
}
