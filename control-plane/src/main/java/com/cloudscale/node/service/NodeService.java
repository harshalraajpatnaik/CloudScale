package com.cloudscale.node.service;

import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.node.dto.NodeDTO;
import com.cloudscale.node.dto.RegisterNodeRequest;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 90;

    private final NodeRepository nodeRepository;
    private final OrganisationRepository organisationRepository;

    public NodeService(NodeRepository nodeRepository, OrganisationRepository organisationRepository) {
        this.nodeRepository = nodeRepository;
        this.organisationRepository = organisationRepository;
    }

    @Transactional
    public NodeDTO registerNode(UUID orgId, RegisterNodeRequest request) {
        if (nodeRepository.existsByHostname(request.hostname())) {
            throw new ConflictException("NODE_HOSTNAME_EXISTS", "A node with hostname '" + request.hostname() + "' is already registered");
        }

        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        Node node = new Node(
                org,
                request.hostname(),
                request.ipAddress(),
                request.totalCpuCores(),
                request.totalMemoryMb(),
                request.simulated()
        );
        node = nodeRepository.save(node);
        log.info("Node registered: {} ({} CPU, {} MB RAM, Simulated: {}) for Org: {}", 
                node.getHostname(), node.getTotalCpuCores(), node.getTotalMemoryMb(), node.isSimulated(), orgId);

        return NodeDTO.fromEntity(node);
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> listNodes(UUID orgId) {
        return nodeRepository.findByOrganisationId(orgId).stream()
                .map(NodeDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public NodeDTO getNode(UUID orgId, UUID nodeId) {
        Node node = nodeRepository.findByIdAndOrganisationId(nodeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND", "Node not found"));
        return NodeDTO.fromEntity(node);
    }

    @Transactional
    public NodeDTO updateNodeStatus(UUID orgId, UUID nodeId, NodeStatus newStatus) {
        Node node = nodeRepository.findByIdAndOrganisationId(nodeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND", "Node not found"));

        NodeStatus current = node.getStatus();
        log.info("Transitioning node {} status: {} -> {}", node.getHostname(), current, newStatus);
        node.setStatus(newStatus);
        node = nodeRepository.save(node);

        return NodeDTO.fromEntity(node);
    }

    @Transactional
    public void recordHeartbeat(UUID nodeId) {
        nodeRepository.findById(nodeId).ifPresent(node -> {
            node.setLastHeartbeat(Instant.now());
            if (node.getStatus() == NodeStatus.UNREACHABLE) {
                node.setStatus(NodeStatus.ACTIVE);
                log.info("Node {} recovered and marked ACTIVE", node.getHostname());
            }
            nodeRepository.save(node);
        });
    }

    @Transactional
    public void deleteNode(UUID orgId, UUID nodeId) {
        Node node = nodeRepository.findByIdAndOrganisationId(nodeId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND", "Node not found"));

        if (node.getAllocatedCpuCores() > 0 || node.getAllocatedMemoryMb() > 0) {
            throw new ConflictException("NODE_NOT_EMPTY", "Cannot decommission node with running workloads. Drain node first.");
        }

        nodeRepository.delete(node);
        log.info("Node decommissioned: {} ({})", node.getHostname(), nodeId);
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void detectUnreachableNodes() {
        Instant threshold = Instant.now().minus(HEARTBEAT_TIMEOUT_SECONDS, ChronoUnit.SECONDS);
        List<Node> staleNodes = nodeRepository.findStaleActiveNodes(threshold);

        for (Node node : staleNodes) {
            log.warn("Node {} heartbeat timed out (>{}s). Marking UNREACHABLE.", node.getHostname(), HEARTBEAT_TIMEOUT_SECONDS);
            node.setStatus(NodeStatus.UNREACHABLE);
            nodeRepository.save(node);
        }
    }
}
