package com.cloudscale.cost;

import com.cloudscale.cost.config.CostRates;
import com.cloudscale.cost.dto.CostSummaryDTO;
import com.cloudscale.cost.dto.NodeCostDTO;
import com.cloudscale.cost.dto.WasteAnalysisDTO;
import com.cloudscale.cost.service.CostService;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.model.WorkloadType;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private WorkloadRepository workloadRepository;

    @Spy
    private CostRates rates = new CostRates();

    @InjectMocks
    private CostService costService;

    private UUID orgId;
    private Organisation org;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        org = new Organisation("FinOps Org", "finops-org");
        org.setId(orgId);
    }

    @Test
    @DisplayName("Calculate burn rate correctly for active workloads")
    void testCalculateCostSummary() {
        Node node1 = new Node(org, "worker-01", "10.0.0.1", 16, 65536L, true);
        when(nodeRepository.findByOrganisationId(orgId)).thenReturn(List.of(node1));

        Workload w1 = new Workload(org, "frontend-gateway", WorkloadType.SERVICE, WorkloadPriority.HIGH, 4, 8192L);
        w1.setActualState(WorkloadState.RUNNING);

        Workload w2 = new Workload(org, "stopped-batch", WorkloadType.BATCH, WorkloadPriority.LOW, 8, 16384L);
        w2.setActualState(WorkloadState.STOPPED); // Should NOT incur running compute burn rate

        when(workloadRepository.findAll()).thenReturn(List.of(w1, w2));

        CostSummaryDTO summary = costService.getCostSummary(orgId);

        // 4 cores * $0.048 = $0.192
        // 8 GB RAM * $0.006 = $0.048
        // Hourly = $0.24
        // Daily = $5.76
        // Monthly = $0.24 * 730 = $175.20
        assertThat(summary.totalCpuCoresAllocated()).isEqualTo(4);
        assertThat(summary.totalMemoryGbAllocated()).isEqualTo(8.0);
        assertThat(summary.activeWorkloadsCount()).isEqualTo(1);
        assertThat(summary.hourlyBurnRateUsd()).isEqualByComparingTo(new BigDecimal("0.2400"));
        assertThat(summary.dailyBurnRateUsd()).isEqualByComparingTo(new BigDecimal("5.76"));
        assertThat(summary.monthlyBurnRateUsd()).isEqualByComparingTo(new BigDecimal("175.20"));
    }

    @Test
    @DisplayName("Detect underutilized compute nodes and quantify dollar waste")
    void testDetectUnderutilizedNodesAndWaste() {
        // Node has 16 cores, only 2 allocated (12.5% utilization -> < 20% underutilized)
        Node n1 = new Node(org, "worker-underutilized", "10.0.0.2", 16, 32768L, true);
        n1.setAllocatedCpuCores(2);
        n1.setAllocatedMemoryMb(4096L);

        when(nodeRepository.findByOrganisationId(orgId)).thenReturn(List.of(n1));

        WasteAnalysisDTO waste = costService.getWasteAnalysis(orgId);

        assertThat(waste.underutilizedNodesCount()).isEqualTo(1);
        assertThat(waste.idleCoresCount()).isEqualTo(14); // 16 - 2 = 14 idle cores
        assertThat(waste.monthlyWastedSpendUsd()).isGreaterThan(BigDecimal.ZERO);
        assertThat(waste.recommendations()).isNotEmpty();
    }
}
