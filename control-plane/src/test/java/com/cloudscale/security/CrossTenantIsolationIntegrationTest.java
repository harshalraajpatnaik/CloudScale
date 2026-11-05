package com.cloudscale.security;

import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.cost.dto.CostSummaryDTO;
import com.cloudscale.cost.service.CostService;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.dto.WorkloadDTO;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.model.WorkloadType;
import com.cloudscale.workload.repository.WorkloadRepository;
import com.cloudscale.workload.service.WorkloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CrossTenantIsolationIntegrationTest {

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private CostService costService;

    private Organisation orgAlpha;
    private Organisation orgBeta;
    private Node nodeAlpha;
    private Node nodeBeta;
    private Workload workloadAlpha;
    private Workload workloadBeta;

    @BeforeEach
    void setUp() {
        String runSuffix = UUID.randomUUID().toString().substring(0, 8);

        orgAlpha = organisationRepository.save(new Organisation("Org Alpha " + runSuffix, "alpha-" + runSuffix));
        orgBeta = organisationRepository.save(new Organisation("Org Beta " + runSuffix, "beta-" + runSuffix));

        nodeAlpha = nodeRepository.save(new Node(orgAlpha, "node-alpha-" + runSuffix, "10.10.1.1", 16, 32768L, true));
        nodeBeta = nodeRepository.save(new Node(orgBeta, "node-beta-" + runSuffix, "10.20.1.1", 32, 65536L, true));

        workloadAlpha = new Workload(orgAlpha, "workload-alpha", WorkloadType.SERVICE, WorkloadPriority.HIGH, 4, 8192L);
        workloadAlpha.setActualState(WorkloadState.RUNNING);
        workloadAlpha.setAssignedNode(nodeAlpha);
        workloadAlpha = workloadRepository.save(workloadAlpha);

        workloadBeta = new Workload(orgBeta, "workload-beta", WorkloadType.SERVICE, WorkloadPriority.CRITICAL, 8, 16384L);
        workloadBeta.setActualState(WorkloadState.RUNNING);
        workloadBeta.setAssignedNode(nodeBeta);
        workloadBeta = workloadRepository.save(workloadBeta);
    }

    @Test
    @DisplayName("Tenant A cannot view Tenant B's workloads in listing")
    void testListWorkloadsTenantIsolation() {
        List<WorkloadDTO> alphaWorkloads = workloadService.listWorkloads(orgAlpha.getId());
        List<WorkloadDTO> betaWorkloads = workloadService.listWorkloads(orgBeta.getId());

        assertThat(alphaWorkloads).extracting(WorkloadDTO::id).contains(workloadAlpha.getId());
        assertThat(alphaWorkloads).extracting(WorkloadDTO::id).doesNotContain(workloadBeta.getId());

        assertThat(betaWorkloads).extracting(WorkloadDTO::id).contains(workloadBeta.getId());
        assertThat(betaWorkloads).extracting(WorkloadDTO::id).doesNotContain(workloadAlpha.getId());
    }

    @Test
    @DisplayName("Tenant A cannot retrieve Tenant B's workload directly by ID")
    void testGetWorkloadCrossTenantRejection() {
        // Org Alpha querying Org Beta's workload ID must throw ResourceNotFoundException
        assertThatThrownBy(() -> workloadService.getWorkload(orgAlpha.getId(), workloadBeta.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workload not found");
    }

    @Test
    @DisplayName("Tenant A cannot stop or deschedule Tenant B's workload")
    void testStopWorkloadCrossTenantRejection() {
        assertThatThrownBy(() -> workloadService.stopWorkload(orgAlpha.getId(), workloadBeta.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cost analytics strictly isolates spend and resource accounting per tenant")
    void testCostAnalyticsTenantIsolation() {
        CostSummaryDTO alphaCost = costService.getCostSummary(orgAlpha.getId());
        CostSummaryDTO betaCost = costService.getCostSummary(orgBeta.getId());

        // Alpha has 1 workload with 4 cores
        assertThat(alphaCost.totalCpuCoresAllocated()).isEqualTo(4);
        assertThat(alphaCost.totalMemoryGbAllocated()).isEqualTo(8.0);
        assertThat(alphaCost.activeWorkloadsCount()).isEqualTo(1);
        assertThat(alphaCost.totalNodesCount()).isEqualTo(1);

        // Beta has 1 workload with 8 cores
        assertThat(betaCost.totalCpuCoresAllocated()).isEqualTo(8);
        assertThat(betaCost.totalMemoryGbAllocated()).isEqualTo(16.0);
        assertThat(betaCost.activeWorkloadsCount()).isEqualTo(1);
        assertThat(betaCost.totalNodesCount()).isEqualTo(1);
    }
}
