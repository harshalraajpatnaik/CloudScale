package com.cloudscale.cost.service;

import com.cloudscale.cost.config.CostRates;
import com.cloudscale.cost.dto.CostSummaryDTO;
import com.cloudscale.cost.dto.NodeCostDTO;
import com.cloudscale.cost.dto.WasteAnalysisDTO;
import com.cloudscale.cost.dto.WorkloadCostDTO;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CostService {

    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;
    private final CostRates rates;

    public CostService(
            NodeRepository nodeRepository,
            WorkloadRepository workloadRepository,
            CostRates rates
    ) {
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
        this.rates = rates;
    }

    public CostSummaryDTO getCostSummary(UUID orgId) {
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        List<Workload> workloads = workloadRepository.findAll().stream()
                .filter(w -> w.getOrganisation().getId().equals(orgId))
                .toList();

        List<Workload> activeWorkloads = workloads.stream()
                .filter(w -> w.getActualState() == WorkloadState.RUNNING || w.getActualState() == WorkloadState.SCHEDULING)
                .toList();

        int totalAllocatedCores = 0;
        long totalAllocatedMemoryMb = 0L;

        for (Workload w : activeWorkloads) {
            totalAllocatedCores += w.getCpuCoresRequired();
            totalAllocatedMemoryMb += w.getMemoryMbRequired();
        }

        double totalMemoryGb = (double) totalAllocatedMemoryMb / 1024.0;

        BigDecimal cpuCostHourly = rates.getCpuCoreHourUsd().multiply(BigDecimal.valueOf(totalAllocatedCores));
        BigDecimal memCostHourly = rates.getMemoryGbHourUsd().multiply(BigDecimal.valueOf(totalMemoryGb));
        BigDecimal hourlyBurn = cpuCostHourly.add(memCostHourly).setScale(4, RoundingMode.HALF_UP);
        BigDecimal dailyBurn = hourlyBurn.multiply(BigDecimal.valueOf(24)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyBurn = hourlyBurn.multiply(rates.getHoursPerMonth()).setScale(2, RoundingMode.HALF_UP);

        return new CostSummaryDTO(
                hourlyBurn,
                dailyBurn,
                monthlyBurn,
                totalAllocatedCores,
                Math.round(totalMemoryGb * 10.0) / 10.0,
                activeWorkloads.size(),
                nodes.size(),
                "USD"
        );
    }

    public List<NodeCostDTO> getNodeCosts(UUID orgId) {
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        List<NodeCostDTO> result = new ArrayList<>();

        for (Node n : nodes) {
            int totalCores = n.getTotalCpuCores();
            int allocatedCores = n.getAllocatedCpuCores();
            double totalMemGb = (double) n.getTotalMemoryMb() / 1024.0;
            double allocatedMemGb = (double) n.getAllocatedMemoryMb() / 1024.0;

            double utilPercent = totalCores > 0 ? ((double) allocatedCores / totalCores) * 100.0 : 0.0;
            boolean isUnderutilized = utilPercent < 20.0;

            BigDecimal cpuHourly = rates.getCpuCoreHourUsd().multiply(BigDecimal.valueOf(totalCores));
            BigDecimal memHourly = rates.getMemoryGbHourUsd().multiply(BigDecimal.valueOf(totalMemGb));
            BigDecimal nodeHourly = cpuHourly.add(memHourly).setScale(4, RoundingMode.HALF_UP);
            BigDecimal nodeMonthly = nodeHourly.multiply(rates.getHoursPerMonth()).setScale(2, RoundingMode.HALF_UP);

            result.add(new NodeCostDTO(
                    n.getId(),
                    n.getHostname(),
                    allocatedCores,
                    totalCores,
                    Math.round(allocatedMemGb * 10.0) / 10.0,
                    Math.round(totalMemGb * 10.0) / 10.0,
                    nodeHourly,
                    nodeMonthly,
                    Math.round(utilPercent * 10.0) / 10.0,
                    isUnderutilized
            ));
        }

        return result;
    }

    public List<WorkloadCostDTO> getWorkloadCosts(UUID orgId) {
        List<Workload> workloads = workloadRepository.findAll().stream()
                .filter(w -> w.getOrganisation().getId().equals(orgId))
                .toList();

        List<WorkloadCostDTO> result = new ArrayList<>();

        for (Workload w : workloads) {
            int cores = w.getCpuCoresRequired();
            double memGb = (double) w.getMemoryMbRequired() / 1024.0;

            BigDecimal cpuHourly = rates.getCpuCoreHourUsd().multiply(BigDecimal.valueOf(cores));
            BigDecimal memHourly = rates.getMemoryGbHourUsd().multiply(BigDecimal.valueOf(memGb));
            BigDecimal hourly = cpuHourly.add(memHourly).setScale(4, RoundingMode.HALF_UP);
            BigDecimal monthly = hourly.multiply(rates.getHoursPerMonth()).setScale(2, RoundingMode.HALF_UP);

            result.add(new WorkloadCostDTO(
                    w.getId(),
                    w.getName(),
                    w.getAssignedNode() != null ? w.getAssignedNode().getHostname() : "UNASSIGNED",
                    cores,
                    Math.round(memGb * 10.0) / 10.0,
                    hourly,
                    monthly,
                    w.getPriority().name(),
                    w.getActualState().name()
            ));
        }

        return result;
    }

    public WasteAnalysisDTO getWasteAnalysis(UUID orgId) {
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        int idleCores = 0;
        long idleMemoryMb = 0L;
        int underutilizedNodesCount = 0;
        int totalCores = 0;

        for (Node n : nodes) {
            int unallocatedCores = Math.max(0, n.getTotalCpuCores() - n.getAllocatedCpuCores());
            long unallocatedMem = Math.max(0L, n.getTotalMemoryMb() - n.getAllocatedMemoryMb());

            double utilPercent = n.getTotalCpuCores() > 0
                    ? ((double) n.getAllocatedCpuCores() / n.getTotalCpuCores()) * 100.0
                    : 0.0;

            if (utilPercent < 20.0) {
                underutilizedNodesCount++;
                idleCores += unallocatedCores;
                idleMemoryMb += unallocatedMem;
            }
            totalCores += n.getTotalCpuCores();
        }

        double idleMemGb = (double) idleMemoryMb / 1024.0;
        BigDecimal cpuWasteHourly = rates.getCpuCoreHourUsd().multiply(BigDecimal.valueOf(idleCores));
        BigDecimal memWasteHourly = rates.getMemoryGbHourUsd().multiply(BigDecimal.valueOf(idleMemGb));
        BigDecimal monthlyWaste = cpuWasteHourly.add(memWasteHourly)
                .multiply(rates.getHoursPerMonth())
                .setScale(2, RoundingMode.HALF_UP);

        double potentialSavings = totalCores > 0 ? ((double) idleCores / totalCores) * 100.0 : 0.0;

        List<String> recommendations = new ArrayList<>();
        if (underutilizedNodesCount > 0) {
            recommendations.add(String.format("Found %d compute node(s) operating below 20%% density. Rebalancing workloads can reduce monthly waste by $%s.",
                    underutilizedNodesCount, monthlyWaste));
            recommendations.add("Enable autonomous autoscaling on batch workloads to scale down compute during off-peak hours.");
            recommendations.add("Consider consolidating workloads onto high-density worker instances to safely decommission idle nodes.");
        } else {
            recommendations.add("Cluster resource utilization is currently well-balanced across all compute nodes.");
        }

        return new WasteAnalysisDTO(
                monthlyWaste,
                idleCores,
                Math.round(idleMemGb * 10.0) / 10.0,
                underutilizedNodesCount,
                Math.round(potentialSavings * 10.0) / 10.0,
                recommendations
        );
    }
}
