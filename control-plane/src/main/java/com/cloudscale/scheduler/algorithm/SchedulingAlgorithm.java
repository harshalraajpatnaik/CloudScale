package com.cloudscale.scheduler.algorithm;

import com.cloudscale.node.entity.Node;
import com.cloudscale.workload.entity.Workload;

import java.util.List;
import java.util.Optional;

public interface SchedulingAlgorithm {

    /**
     * Evaluates candidates and returns the optimal node for placement.
     *
     * @param workload The pending workload to place
     * @param candidates Eligible active nodes
     * @return Selected node, or empty if no node satisfies capacity
     */
    Optional<Node> selectNode(Workload workload, List<Node> candidates);

    String getAlgorithmName();

    String getDescription();
}
