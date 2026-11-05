package com.cloudscale.node;

import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.node.service.NodeService;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NodeHeartbeatFailureDetectorTest {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    private Organisation org;
    private Node node;

    @BeforeEach
    void setUp() {
        String runSuffix = UUID.randomUUID().toString().substring(0, 8);
        org = organisationRepository.save(new Organisation("Heartbeat Org " + runSuffix, "heartbeat-" + runSuffix));
        node = new Node(org, "heartbeat-worker-" + runSuffix, "10.0.1.99", 16, 32768L, false);
        node.setStatus(NodeStatus.ACTIVE);
        node.setLastHeartbeat(Instant.now());
        node = nodeRepository.save(node);
    }

    @Test
    @DisplayName("Heartbeat detector marks stale nodes as UNREACHABLE")
    void testDetectUnreachableStaleNode() {
        // Age heartbeat 120 seconds into the past (threshold is 90s)
        node.setLastHeartbeat(Instant.now().minus(120, ChronoUnit.SECONDS));
        nodeRepository.save(node);

        // Run detection cycle
        nodeService.detectUnreachableNodes();

        Node updated = nodeRepository.findById(node.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NodeStatus.UNREACHABLE);
    }

    @Test
    @DisplayName("Subsequent heartbeat recovers UNREACHABLE node back to ACTIVE state")
    void testNodeHeartbeatRecovery() {
        node.setStatus(NodeStatus.UNREACHABLE);
        nodeRepository.save(node);

        // Record incoming heartbeat
        nodeService.recordHeartbeat(node.getId());

        Node recovered = nodeRepository.findById(node.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(NodeStatus.ACTIVE);
        assertThat(recovered.getLastHeartbeat()).isAfter(Instant.now().minus(5, ChronoUnit.SECONDS));
    }
}
