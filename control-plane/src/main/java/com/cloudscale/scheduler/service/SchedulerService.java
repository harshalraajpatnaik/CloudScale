package com.cloudscale.scheduler.service;

import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.scheduler.algorithm.SchedulingAlgorithm;
import com.cloudscale.workload.dto.WorkloadDTO;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final String DEFAULT_ALGORITHM = "RESOURCE_AWARE";

    private final Map<String, SchedulingAlgorithm> algorithmMap;
    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;

    public SchedulerService(
            Map<String, SchedulingAlgorithm> algorithmMap,
            NodeRepository nodeRepository,
            WorkloadRepository workloadRepository
    ) {
        this.algorithmMap = algorithmMap;
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, noRollbackFor = ConflictException.class)
    public WorkloadDTO schedule(UUID orgId, UUID workloadId, String algorithmName) {
        Workload workload = workloadRepository.findByIdAndOrganisationIdForUpdate(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));

        if (workload.getActualState() == WorkloadState.RUNNING && workload.getAssignedNode() != null) {
            throw new ConflictException("WORKLOAD_ALREADY_RUNNING", "Workload is already running on node " + workload.getAssignedNode().getHostname());
        }

        workload.transitionTo(WorkloadState.SCHEDULING);

        String selectedAlgo = (algorithmName != null && algorithmMap.containsKey(algorithmName.toUpperCase()))
                ? algorithmName.toUpperCase()
                : DEFAULT_ALGORITHM;

        SchedulingAlgorithm algorithm = algorithmMap.get(selectedAlgo);
        log.info("Scheduling workload {} ({} CPU, {} MB) with algorithm '{}'", 
                workload.getName(), workload.getCpuCoresRequired(), workload.getMemoryMbRequired(), selectedAlgo);

        // Fetch eligible active nodes with pessimistic row write-lock
        List<Node> candidates = nodeRepository.findActiveNodesForSchedulingWithLock(orgId);

        Optional<Node> targetNodeOpt = algorithm.selectNode(workload, candidates);

        if (targetNodeOpt.isPresent()) {
            Node targetNode = targetNodeOpt.get();
            // Transactional capacity reservation
            targetNode.allocate(workload.getCpuCoresRequired(), workload.getMemoryMbRequired());
            nodeRepository.save(targetNode);

            workload.setAssignedNode(targetNode);
            workload.setScheduledAt(Instant.now());
            workload.setFailureReason(null);
            workload.transitionTo(WorkloadState.RUNNING);
            workload = workloadRepository.save(workload);

            log.info("Successfully scheduled workload {} -> Node {} (Algorithm: {})",
                    workload.getName(), targetNode.getHostname(), selectedAlgo);

            return WorkloadDTO.fromEntity(workload);
        } else {
            workload.setFailureReason("INSUFFICIENT_NODE_CAPACITY: No eligible active host possesses " 
                    + workload.getCpuCoresRequired() + " CPU cores and " + workload.getMemoryMbRequired() + " MB memory.");
            workload.transitionTo(WorkloadState.FAILED);
            workload = workloadRepository.save(workload);

            log.warn("Failed to schedule workload {}: Insufficient capacity", workload.getName());
            throw new ConflictException("INSUFFICIENT_CAPACITY", workload.getFailureReason());
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WorkloadDTO deschedule(UUID orgId, UUID workloadId) {
        Workload workload = workloadRepository.findByIdAndOrganisationIdForUpdate(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));

        if (workload.getAssignedNode() != null) {
            Node assignedNode = workload.getAssignedNode();
            assignedNode.release(workload.getCpuCoresRequired(), workload.getMemoryMbRequired());
            nodeRepository.save(assignedNode);
            workload.setAssignedNode(null);
        }

        workload.transitionTo(WorkloadState.STOPPED);
        workload = workloadRepository.save(workload);
        log.info("Descheduled workload: {}", workload.getName());

        return WorkloadDTO.fromEntity(workload);
    }
}
