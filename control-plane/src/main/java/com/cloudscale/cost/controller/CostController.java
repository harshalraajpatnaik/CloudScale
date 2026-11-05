package com.cloudscale.cost.controller;

import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.cost.dto.CostSummaryDTO;
import com.cloudscale.cost.dto.NodeCostDTO;
import com.cloudscale.cost.dto.WasteAnalysisDTO;
import com.cloudscale.cost.dto.WorkloadCostDTO;
import com.cloudscale.cost.service.CostService;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cost")
public class CostController {

    private final CostService costService;
    private final OrganisationRepository organisationRepository;

    public CostController(CostService costService, OrganisationRepository organisationRepository) {
        this.costService = costService;
        this.organisationRepository = organisationRepository;
    }

    private UUID getTenantOrgId(Authentication auth) {
        return organisationRepository.findAll().stream()
                .findFirst()
                .map(Organisation::getId)
                .orElse(UUID.randomUUID());
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CostSummaryDTO>> getCostSummary(Authentication auth) {
        UUID orgId = getTenantOrgId(auth);
        CostSummaryDTO summary = costService.getCostSummary(orgId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<List<NodeCostDTO>>> getNodeCosts(Authentication auth) {
        UUID orgId = getTenantOrgId(auth);
        List<NodeCostDTO> nodes = costService.getNodeCosts(orgId);
        return ResponseEntity.ok(ApiResponse.ok(nodes));
    }

    @GetMapping("/workloads")
    public ResponseEntity<ApiResponse<List<WorkloadCostDTO>>> getWorkloadCosts(Authentication auth) {
        UUID orgId = getTenantOrgId(auth);
        List<WorkloadCostDTO> workloads = costService.getWorkloadCosts(orgId);
        return ResponseEntity.ok(ApiResponse.ok(workloads));
    }

    @GetMapping("/waste")
    public ResponseEntity<ApiResponse<WasteAnalysisDTO>> getWasteAnalysis(Authentication auth) {
        UUID orgId = getTenantOrgId(auth);
        WasteAnalysisDTO waste = costService.getWasteAnalysis(orgId);
        return ResponseEntity.ok(ApiResponse.ok(waste));
    }
}
